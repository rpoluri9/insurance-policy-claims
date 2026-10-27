package com.insurance.claims.controller;

import com.insurance.claims.model.Policy;
import com.insurance.claims.model.Requests.CreatePolicyRequest;
import com.insurance.claims.model.Requests.PremiumQuoteRequest;
import com.insurance.claims.model.Requests.UpdatePolicyStatusRequest;
import com.insurance.claims.service.PolicyService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policies;

    public PolicyController(PolicyService policies) {
        this.policies = policies;
    }

    @PostMapping
    public ResponseEntity<Policy> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policies.createPolicy(request));
    }

    @PostMapping("/quote")
    public Map<String, BigDecimal> calculatePremium(@Valid @RequestBody PremiumQuoteRequest request) {
        BigDecimal premium = policies.calculatePremium(
                request.customerId(), request.policyType(), request.coverageAmount());
        return Map.of("premiumAmount", premium);
    }

    @GetMapping
    public List<Policy> listPolicies() {
        return policies.listPolicies();
    }

    @GetMapping("/{id}")
    public Policy getPolicy(@PathVariable Long id) {
        return policies.getPolicy(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<Policy> listPoliciesForCustomer(@PathVariable Long customerId) {
        return policies.listPoliciesForCustomer(customerId);
    }

    @PutMapping("/{id}/status")
    public Policy updateStatus(@PathVariable Long id,
                               @Valid @RequestBody UpdatePolicyStatusRequest request) {
        return policies.updateStatus(id, request.status());
    }
}
