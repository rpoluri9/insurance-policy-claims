package com.insurance.claims.service;

import com.insurance.claims.events.DomainEvent;
import com.insurance.claims.events.EventPublisher;
import com.insurance.claims.exception.InvalidStateException;
import com.insurance.claims.exception.ResourceNotFoundException;
import com.insurance.claims.exception.ValidationException;
import com.insurance.claims.model.Customer;
import com.insurance.claims.model.Policy;
import com.insurance.claims.model.PolicyStatus;
import com.insurance.claims.model.PolicyType;
import com.insurance.claims.model.Requests.CreatePolicyRequest;
import com.insurance.claims.repository.CustomerRepository;
import com.insurance.claims.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PolicyService {

    private static final Map<PolicyStatus, Set<PolicyStatus>> ALLOWED_TRANSITIONS = Map.of(
            PolicyStatus.PENDING, Set.of(PolicyStatus.ACTIVE, PolicyStatus.CANCELLED),
            PolicyStatus.ACTIVE, Set.of(PolicyStatus.EXPIRED, PolicyStatus.CANCELLED, PolicyStatus.RENEWED),
            PolicyStatus.RENEWED, Set.of(PolicyStatus.ACTIVE, PolicyStatus.CANCELLED),
            PolicyStatus.EXPIRED, Set.of(),
            PolicyStatus.CANCELLED, Set.of());

    private final PolicyRepository policies;
    private final CustomerRepository customers;
    private final PremiumCalculator premiumCalculator;
    private final EventPublisher events;

    public PolicyService(PolicyRepository policies, CustomerRepository customers,
                         PremiumCalculator premiumCalculator, EventPublisher events) {
        this.policies = policies;
        this.customers = customers;
        this.premiumCalculator = premiumCalculator;
        this.events = events;
    }

    public Policy createPolicy(CreatePolicyRequest request) {
        Customer customer = customers.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found: " + request.customerId()));
        validateCustomerAge(customer);
        validateDates(request.effectiveDate(), request.renewalDate());

        Policy policy = new Policy();
        policy.setPolicyNumber("POL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        policy.setCustomer(customer);
        policy.setPolicyType(request.policyType());
        policy.setCoverageAmount(request.coverageAmount());
        policy.setPremiumAmount(
                premiumCalculator.calculateFor(customer, request.policyType(), request.coverageAmount()));
        policy.setEffectiveDate(request.effectiveDate());
        policy.setRenewalDate(request.renewalDate());
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setCreatedDate(LocalDate.now());
        Policy saved = policies.save(policy);
        events.publish(new DomainEvent("POLICY_CREATED", "Policy", saved.getPolicyNumber(),
                "type=" + saved.getPolicyType() + " coverage=" + saved.getCoverageAmount()
                        + " premium=" + saved.getPremiumAmount()));
        return saved;
    }

    public BigDecimal calculatePremium(Long customerId, PolicyType type, BigDecimal coverageAmount) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        return premiumCalculator.calculateFor(customer, type, coverageAmount);
    }

    public Policy updateStatus(Long policyId, PolicyStatus newStatus) {
        Policy policy = getPolicy(policyId);
        Set<PolicyStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(policy.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStateException("Cannot move policy from " + policy.getStatus()
                    + " to " + newStatus);
        }
        PolicyStatus oldStatus = policy.getStatus();
        policy.setStatus(newStatus);
        Policy saved = policies.save(policy);
        events.publish(new DomainEvent("POLICY_STATUS_CHANGED", "Policy", saved.getPolicyNumber(),
                "from=" + oldStatus + " to=" + newStatus));
        return saved;
    }

    @Transactional(readOnly = true)
    public Policy getPolicy(Long id) {
        return policies.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Policy> listPolicies() {
        return policies.findAll();
    }

    @Transactional(readOnly = true)
    public List<Policy> listPoliciesForCustomer(Long customerId) {
        customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        return policies.findByCustomerId(customerId);
    }

    private void validateCustomerAge(Customer customer) {
        if (customer.getDateOfBirth() == null) {
            return;
        }
        int age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new ValidationException(
                    "Customer must be at least 18 years old to hold a policy (age: " + age + ")");
        }
    }

    private void validateDates(LocalDate effective, LocalDate renewal) {
        if (!renewal.isAfter(effective)) {
            throw new ValidationException(
                    "Renewal date must be after the effective date");
        }
    }
}
