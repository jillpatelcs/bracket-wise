package ca.bracketwise.api.dto;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.PersonId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CalculateRequest(
        @NotNull HouseholdMode mode,
        String province,
        Integer taxYear,
        BigDecimal monthlySavings,
        Boolean fhsaFirstHomePlan,
        @Valid AssumptionsDto assumptions,
        @NotEmpty @Valid List<PersonDto> persons,
        @Valid List<AllocationPercentDto> userAllocation
) {
    public enum HouseholdMode { SINGLE, COUPLE }

    public record AssumptionsDto(BigDecimal expectedAnnualReturn, BigDecimal rrspWithdrawalTaxRate) {}

    public record PersonDto(
            PersonId id,
            String label,
            Boolean isNewcomer,
            LocalDate arrivalDate,
            LocalDate graduationDate,
            LocalDate employmentStartDate,
            Boolean canadianTaxResidentForFullYear,
            BigDecimal estimatedEmploymentIncomeFor2026,
            BigDecimal availableTfsaRoom,
            BigDecimal availableRrspDeductionLimit,
            Boolean fhsaEligible,
            BigDecimal availableFhsaParticipationRoom
    ) {}

    public record AllocationPercentDto(PersonId person, Account account, BigDecimal percent) {}
}
