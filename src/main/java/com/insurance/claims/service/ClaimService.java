package com.insurance.claims.service;

import com.insurance.claims.events.DomainEvent;
import com.insurance.claims.events.EventPublisher;
import com.insurance.claims.exception.InvalidStateException;
import com.insurance.claims.exception.ResourceNotFoundException;
import com.insurance.claims.exception.ValidationException;
import com.insurance.claims.model.Claim;
import com.insurance.claims.model.ClaimStatus;
import com.insurance.claims.model.Policy;
import com.insurance.claims.model.PolicyStatus;
import com.insurance.claims.model.Requests.CreateClaimRequest;
import com.insurance.claims.repository.ClaimRepository;
import com.insurance.claims.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Claim lifecycle: REGISTERED -> UNDER_REVIEW -> APPROVED -> SETTLED,
 * with REJECTED as a terminal branch from REGISTERED or UNDER_REVIEW.
 * Any other transition is rejected, so a claim can never jump states or
 * be settled twice.
 */
@Service
@Transactional
public class ClaimService {

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED_TRANSITIONS = Map.of(
            ClaimStatus.REGISTERED, Set.of(ClaimStatus.UNDER_REVIEW, ClaimStatus.REJECTED),
            ClaimStatus.UNDER_REVIEW, Set.of(ClaimStatus.APPROVED, ClaimStatus.REJECTED),
            ClaimStatus.APPROVED, Set.of(ClaimStatus.SETTLED),
            ClaimStatus.REJECTED, Set.of(),
            ClaimStatus.SETTLED, Set.of());

    private final ClaimRepository claims;
    private final PolicyRepository policies;
    private final EventPublisher events;

    public ClaimService(ClaimRepository claims, PolicyRepository policies, EventPublisher events) {
        this.claims = claims;
        this.policies = policies;
        this.events = events;
    }

    public Claim registerClaim(CreateClaimRequest request) {
        Policy policy = policies.findById(request.policyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found: " + request.policyId()));
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new ValidationException("Claims can only be filed against an ACTIVE policy "
                    + "(policy status: " + policy.getStatus() + ")");
        }
        if (request.incidentDate().isBefore(policy.getEffectiveDate())
                || request.incidentDate().isAfter(policy.getRenewalDate())) {
            throw new ValidationException("Incident date must fall within the policy period "
                    + policy.getEffectiveDate() + " to " + policy.getRenewalDate());
        }
        BigDecimal remaining = remainingCoverage(policy.getId(), null);
        if (request.claimAmount().compareTo(remaining) > 0) {
            throw new ValidationException("Claim amount " + request.claimAmount()
                    + " exceeds remaining coverage " + remaining);
        }
        Claim claim = new Claim();
        claim.setClaimNumber("CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        claim.setPolicy(policy);
        claim.setDescription(request.description());
        claim.setClaimAmount(request.claimAmount());
        claim.setIncidentDate(request.incidentDate());
        claim.setStatus(ClaimStatus.REGISTERED);
        claim.setFiledDate(LocalDate.now());
        Claim saved = claims.save(claim);
        events.publish(new DomainEvent("CLAIM_REGISTERED", "Claim", saved.getClaimNumber(),
                "policy=" + policy.getPolicyNumber() + " amount=" + saved.getClaimAmount()));
        return saved;
    }

    public Claim updateStatus(Long claimId, ClaimStatus newStatus, String reason) {
        Claim claim = getClaim(claimId);
        Set<ClaimStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(claim.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStateException("Cannot move claim from " + claim.getStatus()
                    + " to " + newStatus);
        }
        ClaimStatus oldStatus = claim.getStatus();
        claim.setStatus(newStatus);
        claim.setDecisionReason(reason);
        Claim saved = claims.save(claim);
        events.publish(new DomainEvent("CLAIM_STATUS_CHANGED", "Claim", saved.getClaimNumber(),
                "from=" + oldStatus + " to=" + newStatus
                        + (reason != null ? " reason=" + reason : "")));
        return saved;
    }

    /**
     * Settles an APPROVED claim: the payout is the claim amount capped at the
     * coverage still unused by other approved/settled claims on the policy.
     */
    public Claim settleClaim(Long claimId) {
        Claim claim = getClaim(claimId);
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw new InvalidStateException(
                    "Only an APPROVED claim can be settled (current status: " + claim.getStatus() + ")");
        }
        BigDecimal remaining = remainingCoverage(claim.getPolicy().getId(), claim.getId());
        BigDecimal payout = claim.getClaimAmount().min(remaining);
        claim.setSettledAmount(payout);
        claim.setStatus(ClaimStatus.SETTLED);
        Claim saved = claims.save(claim);
        events.publish(new DomainEvent("CLAIM_SETTLED", "Claim", saved.getClaimNumber(),
                "payout=" + payout));
        return saved;
    }

    @Transactional(readOnly = true)
    public Claim getClaim(Long id) {
        return claims.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Claim> listClaims() {
        return claims.findAll();
    }

    @Transactional(readOnly = true)
    public List<Claim> listClaimsForPolicy(Long policyId) {
        policies.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        return claims.findByPolicyId(policyId);
    }

    /**
     * Coverage left on a policy: coverage amount minus the amounts of claims
     * already APPROVED or SETTLED (optionally excluding one claim, used when
     * settling that claim itself).
     */
    public BigDecimal remainingCoverage(Long policyId, Long excludeClaimId) {
        Policy policy = policies.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        List<Claim> committed = claims.findByPolicyIdAndStatusIn(
                policyId, List.of(ClaimStatus.APPROVED, ClaimStatus.SETTLED));
        BigDecimal used = BigDecimal.ZERO;
        for (Claim c : committed) {
            if (excludeClaimId != null && excludeClaimId.equals(c.getId())) {
                continue;
            }
            used = used.add(c.getClaimAmount());
        }
        BigDecimal remaining = policy.getCoverageAmount().subtract(used);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }
}
