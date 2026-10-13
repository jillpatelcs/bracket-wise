package ca.bracketwise.domain.calc;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.DomainException;
import ca.bracketwise.domain.Money;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.allocation.PercentConverter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public final class HouseholdValidator {

    public static void validate(HouseholdInput input, PlanningDefaults defaults) {
        if (!defaults.supportedProvince().equals(input.province())) {
            throw new DomainException("UNSUPPORTED_PROVINCE", "province", "Only Ontario (ON) is supported.");
        }
        if (input.taxYear() != defaults.supportedTaxYear()) {
            throw new DomainException("UNSUPPORTED_TAX_YEAR", "taxYear", "Only the 2026 planning year is supported.");
        }
        int expected = input.mode() == HouseholdInput.Mode.SINGLE ? 1 : 2;
        if (input.persons() == null || input.persons().size() != expected) {
            throw new DomainException("WRONG_PERSON_COUNT", "persons",
                    "SINGLE requires one person and COUPLE requires two people.");
        }
        Set<PersonId> ids = new HashSet<>();
        boolean anyFhsa = false;
        for (int i = 0; i < input.persons().size(); i++) {
            HouseholdInput.PersonInput p = input.persons().get(i);
            validatePerson(p, i, defaults);
            if (!ids.add(p.id())) {
                throw new DomainException("WRONG_PERSON_COUNT", "persons[" + i + "].id", "Person ids must be unique.");
            }
            if (input.mode() == HouseholdInput.Mode.SINGLE && p.id() != PersonId.A) {
                throw new DomainException("WRONG_PERSON_COUNT", "persons[0].id", "The single person must use id A.");
            }
            anyFhsa = anyFhsa || p.fhsaEligible();
        }
        if (input.mode() == HouseholdInput.Mode.COUPLE
                && (ids.size() != 2 || !ids.contains(PersonId.A) || !ids.contains(PersonId.B))) {
            throw new DomainException("WRONG_PERSON_COUNT", "persons", "Couple mode requires persons A and B.");
        }
        if (input.fhsaFirstHomePlan() && !anyFhsa) {
            throw new DomainException("FIRST_HOME_PLAN_WITHOUT_ELIGIBLE_FHSA", "fhsaFirstHomePlan",
                    "The first-home planning assumption requires at least one FHSA-eligible person.");
        }
        validateMoney("monthlySavings", input.monthlySavings(), BigDecimal.ZERO, defaults.maxMonthlySavings());
        HouseholdInput.Assumptions a = input.assumptions();
        if (a == null || a.expectedAnnualReturn() == null || a.rrspWithdrawalTaxRate() == null) {
            throw new DomainException("INVALID_ASSUMPTION", "assumptions", "Assumptions are required.");
        }
        validateRate("assumptions.expectedAnnualReturn", a.expectedAnnualReturn(),
                defaults.minExpectedAnnualReturn(), defaults.maxExpectedAnnualReturn());
        validateRate("assumptions.rrspWithdrawalTaxRate", a.rrspWithdrawalTaxRate(),
                defaults.minRrspWithdrawalTaxRate(), defaults.maxRrspWithdrawalTaxRate());
        if (input.userAllocation() != null) {
            for (PercentConverter.PercentLine line : input.userAllocation()) {
                if (line.percent().compareTo(BigDecimal.ZERO) < 0) {
                    throw new DomainException("NEGATIVE_VALUE", "userAllocation", "Percentages cannot be negative.");
                }
                if (Money.hasMoreThanTwoDecimals(line.percent())) {
                    throw new DomainException("NEGATIVE_VALUE", "userAllocation", "Percentages may have at most 2 decimals.");
                }
                boolean known = input.persons().stream().anyMatch(p -> p.id() == line.person());
                if (!known) {
                    throw new DomainException("WRONG_PERSON_COUNT", "userAllocation", "Allocation refers to an unknown person.");
                }
            }
        }
    }

    private static void validatePerson(HouseholdInput.PersonInput p, int i, PlanningDefaults defaults) {
        String prefix = "persons[" + i + "]";
        if (p.label() != null && p.label().length() > 20) {
            throw new DomainException("NEGATIVE_VALUE", prefix + ".label", "Label must be at most 20 characters.");
        }
        validateMoney(prefix + ".estimatedEmploymentIncomeFor2026", p.estimatedEmploymentIncomeFor2026(),
                BigDecimal.ZERO, defaults.maxIncome());
        validateMoney(prefix + ".availableTfsaRoom", p.availableTfsaRoom(), BigDecimal.ZERO, defaults.maxRoom());
        validateMoney(prefix + ".availableRrspDeductionLimit", p.availableRrspDeductionLimit(), BigDecimal.ZERO, defaults.maxRoom());
        if (p.availableFhsaParticipationRoom() == null) {
            throw new DomainException("MISSING_ROOM_VALUE", prefix + ".availableFhsaParticipationRoom",
                    "FHSA participation room is required.");
        }
        validateMoney(prefix + ".availableFhsaParticipationRoom", p.availableFhsaParticipationRoom(),
                BigDecimal.ZERO, defaults.maxRoom());
        if (!p.fhsaEligible() && p.availableFhsaParticipationRoom().compareTo(BigDecimal.ZERO) != 0) {
            throw new DomainException("FHSA_ROOM_WITHOUT_ELIGIBILITY", prefix + ".availableFhsaParticipationRoom",
                    "FHSA room must be 0 when the person is not FHSA-eligible.");
        }
    }

    private static void validateMoney(String field, BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            throw new DomainException("MISSING_ROOM_VALUE", field, "This value is required.");
        }
        if (Money.hasMoreThanTwoDecimals(value)) {
            throw new DomainException("NEGATIVE_VALUE", field, "At most two decimal places are allowed.");
        }
        if (value.compareTo(min) < 0) {
            throw new DomainException("NEGATIVE_VALUE", field, "This value cannot be negative.");
        }
        if (value.compareTo(max) > 0) {
            throw new DomainException("NEGATIVE_VALUE", field, "This value exceeds the supported maximum.");
        }
    }

    private static void validateRate(String field, BigDecimal value, BigDecimal min, BigDecimal max) {
        if (Money.hasMoreThanNDecimals(value.stripTrailingZeros(), 4) && value.scale() > 4) {
            throw new DomainException("INVALID_ASSUMPTION", field, "This assumption has too many decimal places.");
        }
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new DomainException("INVALID_ASSUMPTION", field, "This assumption is outside the allowed range.");
        }
    }

    public static long totalRoomCents(HouseholdInput input) {
        long total = 0;
        for (HouseholdInput.PersonInput p : input.persons()) {
            total += Money.toCents(p.availableTfsaRoom());
            total += Money.toCents(p.availableRrspDeductionLimit());
            if (p.fhsaEligible()) {
                total += Money.toCents(p.availableFhsaParticipationRoom());
            }
        }
        return total;
    }
}
