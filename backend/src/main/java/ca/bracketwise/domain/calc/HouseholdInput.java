package ca.bracketwise.domain.calc;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.allocation.PercentConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HouseholdInput(
        Mode mode,
        String province,
        int taxYear,
        BigDecimal monthlySavings,
        boolean fhsaFirstHomePlan,
        Assumptions assumptions,
        List<PersonInput> persons,
        List<PercentConverter.PercentLine> userAllocation
) {
    public enum Mode { SINGLE, COUPLE }

    public record Assumptions(BigDecimal expectedAnnualReturn, BigDecimal rrspWithdrawalTaxRate) {}

    public record PersonInput(
            PersonId id,
            String label,
            boolean isNewcomer,
            LocalDate arrivalDate,
            LocalDate graduationDate,
            LocalDate employmentStartDate,
            boolean canadianTaxResidentForFullYear,
            BigDecimal estimatedEmploymentIncomeFor2026,
            BigDecimal availableTfsaRoom,
            BigDecimal availableRrspDeductionLimit,
            boolean fhsaEligible,
            BigDecimal availableFhsaParticipationRoom
    ) {
        public BigDecimal room(Account account) {
            return switch (account) {
                case TFSA -> availableTfsaRoom;
                case RRSP -> availableRrspDeductionLimit;
                case FHSA -> availableFhsaParticipationRoom;
            };
        }
    }
}
