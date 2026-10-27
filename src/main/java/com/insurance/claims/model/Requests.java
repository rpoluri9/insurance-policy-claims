package com.insurance.claims.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTOs for the REST API. Kept in one file: they are small,
 * validated with Bean Validation, and never persisted directly.
 */
public final class Requests {

    private Requests() {
    }

    public record CreateCustomerRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            String phone,
            @Past LocalDate dateOfBirth,
            String address) {
    }

    public record CreatePolicyRequest(
            @NotNull Long customerId,
            @NotNull PolicyType policyType,
            @NotNull @Positive BigDecimal coverageAmount,
            @NotNull LocalDate effectiveDate,
            @NotNull LocalDate renewalDate) {
    }

    public record PremiumQuoteRequest(
            @NotNull Long customerId,
            @NotNull PolicyType policyType,
            @NotNull @Positive BigDecimal coverageAmount) {
    }

    public record UpdatePolicyStatusRequest(
            @NotNull PolicyStatus status) {
    }

    public record CreateClaimRequest(
            @NotNull Long policyId,
            @NotBlank String description,
            @NotNull @Positive BigDecimal claimAmount,
            @NotNull LocalDate incidentDate) {
    }

    public record UpdateClaimStatusRequest(
            @NotNull ClaimStatus status,
            String reason) {
    }
}
