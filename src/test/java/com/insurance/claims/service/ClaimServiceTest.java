package com.insurance.claims.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.insurance.claims.events.DomainEvent;
import com.insurance.claims.events.EventPublisher;
import com.insurance.claims.exception.InvalidStateException;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private EventPublisher events;

    private ClaimService claims;

    private Policy activePolicy;

    @BeforeEach
    void setUp() {
        claims = new ClaimService(claimRepository, policyRepository, events);
        activePolicy = new Policy();
        activePolicy.setId(10L);
        activePolicy.setPolicyNumber("POL-TEST1234");
        activePolicy.setStatus(PolicyStatus.ACTIVE);
        activePolicy.setCoverageAmount(new BigDecimal("100000"));
        activePolicy.setEffectiveDate(LocalDate.of(2026, 1, 1));
        activePolicy.setRenewalDate(LocalDate.of(2027, 1, 1));
    }

    private CreateClaimRequest request(BigDecimal amount) {
        return new CreateClaimRequest(10L, "Hospital bill", amount, LocalDate.of(2026, 3, 10));
    }

    @Test
    void registerClaimSetsRegisteredStatusAndClaimNumber() {
        when(policyRepository.findById(10L)).thenReturn(Optional.of(activePolicy));
        when(claimRepository.findByPolicyIdAndStatusIn(10L,
                List.of(ClaimStatus.APPROVED, ClaimStatus.SETTLED))).thenReturn(List.of());
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim saved = claims.registerClaim(request(new BigDecimal("5000")));

        assertEquals(ClaimStatus.REGISTERED, saved.getStatus());
        assertEquals(true, saved.getClaimNumber().startsWith("CLM-"));
        verify(events).publish(any(DomainEvent.class));
    }

    @Test
    void registerClaimRejectsNonActivePolicy() {
        activePolicy.setStatus(PolicyStatus.CANCELLED);
        when(policyRepository.findById(10L)).thenReturn(Optional.of(activePolicy));
        assertThrows(ValidationException.class,
                () -> claims.registerClaim(request(new BigDecimal("5000"))));
    }

    @Test
    void registerClaimRejectsIncidentOutsidePolicyPeriod() {
        when(policyRepository.findById(10L)).thenReturn(Optional.of(activePolicy));
        CreateClaimRequest bad = new CreateClaimRequest(10L, "Old incident",
                new BigDecimal("1000"), LocalDate.of(2025, 6, 1));
        assertThrows(ValidationException.class, () -> claims.registerClaim(bad));
    }

    @Test
    void registerClaimRejectsAmountAboveRemainingCoverage() {
        Claim approved = new Claim();
        approved.setClaimAmount(new BigDecimal("90000"));
        when(policyRepository.findById(10L)).thenReturn(Optional.of(activePolicy));
        when(claimRepository.findByPolicyIdAndStatusIn(10L,
                List.of(ClaimStatus.APPROVED, ClaimStatus.SETTLED))).thenReturn(List.of(approved));
        assertThrows(ValidationException.class,
                () -> claims.registerClaim(request(new BigDecimal("20000"))));
    }

    @Test
    void updateStatusAllowsRegisteredToUnderReview() {
        Claim claim = new Claim();
        claim.setId(5L);
        claim.setStatus(ClaimStatus.REGISTERED);
        when(claimRepository.findById(5L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(ClaimStatus.UNDER_REVIEW,
                claims.updateStatus(5L, ClaimStatus.UNDER_REVIEW, "Checking documents").getStatus());
    }

    @Test
    void updateStatusRejectsJumpToSettled() {
        Claim claim = new Claim();
        claim.setId(5L);
        claim.setStatus(ClaimStatus.REGISTERED);
        when(claimRepository.findById(5L)).thenReturn(Optional.of(claim));
        assertThrows(InvalidStateException.class,
                () -> claims.updateStatus(5L, ClaimStatus.SETTLED, null));
    }

    @Test
    void settlePaysClaimAmountCappedAtRemainingCoverage() {
        Claim claim = new Claim();
        claim.setId(6L);
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setClaimAmount(new BigDecimal("5000"));
        claim.setPolicy(activePolicy);
        Claim other = new Claim();
        other.setId(7L);
        other.setClaimAmount(new BigDecimal("10000"));
        when(claimRepository.findById(6L)).thenReturn(Optional.of(claim));
        when(policyRepository.findById(10L)).thenReturn(Optional.of(activePolicy));
        when(claimRepository.findByPolicyIdAndStatusIn(10L,
                List.of(ClaimStatus.APPROVED, ClaimStatus.SETTLED)))
                .thenReturn(List.of(claim, other));
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim settled = claims.settleClaim(6L);
        assertEquals(ClaimStatus.SETTLED, settled.getStatus());
        assertEquals(new BigDecimal("5000"), settled.getSettledAmount());
    }

    @Test
    void settleRejectsNonApprovedClaim() {
        Claim claim = new Claim();
        claim.setId(6L);
        claim.setStatus(ClaimStatus.REGISTERED);
        when(claimRepository.findById(6L)).thenReturn(Optional.of(claim));
        assertThrows(InvalidStateException.class, () -> claims.settleClaim(6L));
    }
}
