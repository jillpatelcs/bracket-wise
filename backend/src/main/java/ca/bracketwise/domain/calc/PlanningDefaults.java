package ca.bracketwise.domain.calc;

import java.math.BigDecimal;

public record PlanningDefaults(
        int effectiveTaxYear,
        String version,
        String source,
        String retrievedOn,
        BigDecimal allocationIncrement,
        BigDecimal defaultExpectedAnnualReturn,
        BigDecimal defaultRrspWithdrawalTaxRate,
        BigDecimal minExpectedAnnualReturn,
        BigDecimal maxExpectedAnnualReturn,
        BigDecimal minRrspWithdrawalTaxRate,
        BigDecimal maxRrspWithdrawalTaxRate,
        BigDecimal maxMonthlySavings,
        BigDecimal maxIncome,
        BigDecimal maxRoom,
        int maxRequestBytes,
        int horizonYears,
        String supportedProvince,
        int supportedTaxYear
) {}
