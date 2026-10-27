package com.insurance.claims.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EventPublisher events;

    private PolicyService policies;

    private Customer adult;

    @BeforeEach
    void setUp() {
        policies = new PolicyService(policyRepository, customerRepository,
                new PremiumCalculator(), events);
        adult = new Customer("Ravi", "Kumar", "ravi@example.com", "555-0101",
                LocalDate.of(1990, 5, 15), "2 Oak Ave");
        adult.setId(1L);
    }

    private CreatePolicyRequest request() {
        return new CreatePolicyRequest(1L, PolicyType.HEALTH, new BigDecimal("100000"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));
    }

    @Test
    void createPolicySetsActiveStatusPremiumAndPolicyNumber() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(adult));
        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Policy saved = policies.createPolicy(request());

        assertEquals(PolicyStatus.ACTIVE, saved.getStatus());
        assertEquals(new BigDecimal("5000.00"), saved.getPremiumAmount());
        assertEquals(true, saved.getPolicyNumber().startsWith("POL-"));
        verify(events).publish(any(DomainEvent.class));
    }

    @Test
    void createPolicyRejectsUnknownCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> policies.createPolicy(request()));
    }

    @Test
    void createPolicyRejectsUnderageCustomer() {
        Customer minor = new Customer("Teen", "User", "teen@example.com", "555-0102",
                LocalDate.now().minusYears(16), "3 Pine Rd");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(minor));
        assertThrows(ValidationException.class, () -> policies.createPolicy(request()));
    }

    @Test
    void createPolicyRejectsRenewalBeforeEffective() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(adult));
        CreatePolicyRequest bad = new CreatePolicyRequest(1L, PolicyType.AUTO,
                new BigDecimal("50000"), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1));
        assertThrows(ValidationException.class, () -> policies.createPolicy(bad));
    }

    @Test
    void updateStatusAllowsActiveToCancelled() {
        Policy policy = new Policy();
        policy.setId(7L);
        policy.setStatus(PolicyStatus.ACTIVE);
        when(policyRepository.findById(7L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        Policy updated = policies.updateStatus(7L, PolicyStatus.CANCELLED);
        assertEquals(PolicyStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void updateStatusRejectsIllegalTransition() {
        Policy policy = new Policy();
        policy.setId(7L);
        policy.setStatus(PolicyStatus.ACTIVE);
        when(policyRepository.findById(7L)).thenReturn(Optional.of(policy));

        assertThrows(InvalidStateException.class,
                () -> policies.updateStatus(7L, PolicyStatus.PENDING));
    }

    @Test
    void premiumQuoteAppliesSeniorFactor() {
        Customer senior = new Customer("Elder", "Singh", "elder@example.com", "555-0103",
                LocalDate.now().minusYears(70), "4 Elm St");
        when(customerRepository.findById(9L)).thenReturn(Optional.of(senior));
        assertEquals(new BigDecimal("6000.00"),
                policies.calculatePremium(9L, PolicyType.HEALTH, new BigDecimal("100000")));
    }
}
