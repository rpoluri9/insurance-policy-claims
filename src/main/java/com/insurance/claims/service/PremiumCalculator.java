package com.insurance.claims.service;

import com.insurance.claims.model.Customer;
import com.insurance.claims.model.PolicyType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Premium calculation from policy attributes. The formula is deliberately
 * transparent and documented, because premium logic is the part of an
 * insurance system an interviewer or auditor will always ask about:
 *
 *   premium = coverageAmount x baseRate(policyType) x ageFactor(age)
 *
 * Base annual rates: HEALTH 5%, AUTO 4%, HOME 3%, LIFE 2.5%, TRAVEL 6%.
 * Age factor: under 25 -> 1.10, 60 and over -> 1.20, otherwise 1.00.
 * Result is rounded to 2 decimal places (HALF_UP).
 */
@Component
public class PremiumCalculator {

    private static final Map<PolicyType, BigDecimal> BASE_RATES = Map.of(
            PolicyType.HEALTH, new BigDecimal("0.05"),
            PolicyType.AUTO, new BigDecimal("0.04"),
            PolicyType.HOME, new BigDecimal("0.03"),
            PolicyType.LIFE, new BigDecimal("0.025"),
            PolicyType.TRAVEL, new BigDecimal("0.06"));

    public BigDecimal calculate(PolicyType type, BigDecimal coverageAmount, int ageYears) {
        BigDecimal rate = BASE_RATES.get(type);
        if (rate == null) {
            throw new IllegalArgumentException("Unknown policy type: " + type);
        }
        BigDecimal factor = ageFactor(ageYears);
        return coverageAmount.multiply(rate).multiply(factor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFor(Customer customer, PolicyType type, BigDecimal coverageAmount) {
        int age = ageOf(customer);
        return calculate(type, coverageAmount, age);
    }

    public int ageOf(Customer customer) {
        if (customer.getDateOfBirth() == null) {
            return 30; // neutral default when no birth date is on file
        }
        return Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
    }

    private BigDecimal ageFactor(int ageYears) {
        if (ageYears >= 60) {
            return new BigDecimal("1.20");
        }
        if (ageYears < 25) {
            return new BigDecimal("1.10");
        }
        return BigDecimal.ONE;
    }
}
