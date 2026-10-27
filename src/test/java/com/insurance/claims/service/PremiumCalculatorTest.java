package com.insurance.claims.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.insurance.claims.model.Customer;
import com.insurance.claims.model.PolicyType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PremiumCalculatorTest {

    private final PremiumCalculator calculator = new PremiumCalculator();

    @Test
    void healthBaseRateIsFivePercent() {
        assertEquals(new BigDecimal("5000.00"),
                calculator.calculate(PolicyType.HEALTH, new BigDecimal("100000"), 36));
    }

    @Test
    void autoBaseRateIsFourPercent() {
        assertEquals(new BigDecimal("2000.00"),
                calculator.calculate(PolicyType.AUTO, new BigDecimal("50000"), 40));
    }

    @Test
    void seniorAgeFactorAddsTwentyPercent() {
        assertEquals(new BigDecimal("6000.00"),
                calculator.calculate(PolicyType.HEALTH, new BigDecimal("100000"), 65));
    }

    @Test
    void youngAgeFactorAddsTenPercent() {
        assertEquals(new BigDecimal("1100.00"),
                calculator.calculate(PolicyType.AUTO, new BigDecimal("25000"), 22));
    }

    @Test
    void ageBoundarySixtyGetsSeniorFactor() {
        assertEquals(new BigDecimal("3600.00"),
                calculator.calculate(PolicyType.HOME, new BigDecimal("100000"), 60));
    }

    @Test
    void resultIsRoundedToTwoDecimals() {
        assertEquals(new BigDecimal("833.33"),
                calculator.calculate(PolicyType.LIFE, new BigDecimal("33333"), 30));
    }

    @Test
    void ageIsDerivedFromDateOfBirth() {
        Customer customer = new Customer("Asha", "Patel", "asha@example.com", "555-0100",
                LocalDate.now().minusYears(70), "1 Main St");
        assertEquals(70, calculator.ageOf(customer));
    }
}
