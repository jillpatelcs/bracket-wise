package ca.bracketwise.domain.allocation;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.PersonId;

import java.math.BigDecimal;

public record ExplanationStep(
        int step,
        PersonId person,
        Account account,
        AllocationRule rule,
        BigDecimal amount,
        int incrementCount,
        boolean deductible,
        BigDecimal estimatedTaxSavings,
        BigDecimal effectiveSavingsRate,
        BigDecimal savingsRateMin,
        BigDecimal savingsRateMax,
        NextIncrement nextIncrement,
        String reason
) {
    public record NextIncrement(BigDecimal amount, BigDecimal estimatedTaxSavings, BigDecimal savingsRate) {}
}
