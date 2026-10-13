package ca.bracketwise.domain.calc;

import ca.bracketwise.Fixtures;
import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.DomainException;
import ca.bracketwise.domain.Money;
import ca.bracketwise.domain.PersonId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioCalculatorTest {
    private final ScenarioCalculator calc = new ScenarioCalculator(Fixtures.SYNTHETIC, Fixtures.DEFAULTS);

    @Test
    void exampleCoupleMatchesFixtureTotals() {
        CalculationResult r = calc.calculate(Fixtures.exampleCouple());
        assertEquals("synthetic-fixture-1", r.configVersion());
        assertEquals("18000.00", Money.asString(r.assumptions().annualBudget()));
        assertEquals("1950.00", Money.asString(r.estimatedTaxSavings().userDefined().annualTotal()));
        assertEquals("1800.00", Money.asString(r.estimatedTaxSavings().modeled().annualTotal()));
        assertEquals("9750.00", Money.asString(r.estimatedTaxSavings().userDefined().cumulativeFiveYears()));
        assertEquals("3600.00", Money.asString(r.userAllocation().allocations().stream()
                .filter(l -> l.person() == PersonId.A && l.account() == Account.TFSA)
                .findFirst().orElseThrow().amount()));
        assertEquals("104434.44", r.projection().userDefined().householdPretaxByYear().get(4).toPlainString());
        assertEquals("98168.37", r.projection().userDefined().estimatedAfterTaxAccountValue().toPlainString());
        assertEquals("10774.99", r.projection().userDefined().taxSavingsIfSaved().toPlainString());
        assertEquals("103274.09", r.projection().modeled().estimatedAfterTaxAccountValue().toPlainString());
        assertEquals("9946.15", r.projection().modeled().taxSavingsIfSaved().toPlainString());
    }

    @Test
    void swapPartnersSwapsTax() {
        HouseholdInput base = Fixtures.exampleCouple();
        HouseholdInput.PersonInput a = base.persons().get(0);
        HouseholdInput.PersonInput b = base.persons().get(1);
        HouseholdInput swapped = new HouseholdInput(
                base.mode(), base.province(), base.taxYear(), base.monthlySavings(), base.fhsaFirstHomePlan(),
                base.assumptions(),
                List.of(
                        new HouseholdInput.PersonInput(PersonId.A, "A", b.isNewcomer(), b.arrivalDate(), null, null,
                                b.canadianTaxResidentForFullYear(), b.estimatedEmploymentIncomeFor2026(),
                                b.availableTfsaRoom(), b.availableRrspDeductionLimit(), b.fhsaEligible(),
                                b.availableFhsaParticipationRoom()),
                        new HouseholdInput.PersonInput(PersonId.B, "B", a.isNewcomer(), a.arrivalDate(), null, null,
                                a.canadianTaxResidentForFullYear(), a.estimatedEmploymentIncomeFor2026(),
                                a.availableTfsaRoom(), a.availableRrspDeductionLimit(), a.fhsaEligible(),
                                a.availableFhsaParticipationRoom())
                ),
                null
        );
        BigDecimal taxA = calc.calculate(base).perPersonTax().get(0).components().total();
        BigDecimal taxBswapped = calc.calculate(swapped).perPersonTax().get(1).components().total();
        assertEquals(taxA, taxBswapped);
    }

    @Test
    void householdTaxEqualsSum() {
        CalculationResult r = calc.calculate(Fixtures.exampleCouple());
        BigDecimal sum = r.perPersonTax().stream()
                .map(t -> t.components().total())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("12850.00"), sum);
    }

    @Test
    void partYearDisablesTax() {
        CalculationResult r = calc.calculate(Fixtures.exampleCouple(false, true));
        assertEquals("TAX_OPTIMIZATION_DISABLED_PART_YEAR", r.warnings().get(0).code());
        assertNull(r.perPersonTax());
        assertNull(r.modeledAllocation());
        assertNull(r.estimatedTaxSavings());
        org.junit.jupiter.api.Assertions.assertNotNull(r.projection().userDefined());
    }

    @Test
    void firstHomeWithoutEligibleRejected() {
        HouseholdInput bad = new HouseholdInput(
                HouseholdInput.Mode.SINGLE, "ON", 2026, new BigDecimal("100"),
                true,
                new HouseholdInput.Assumptions(new BigDecimal("0.04"), new BigDecimal("0.20")),
                List.of(Fixtures.person(PersonId.A, "A", new BigDecimal("50000"), "1000", "1000", false, "0", true)),
                null
        );
        DomainException ex = assertThrows(DomainException.class, () -> calc.calculate(bad));
        assertEquals("FIRST_HOME_PLAN_WITHOUT_ELIGIBLE_FHSA", ex.code());
    }

    @Test
    void unsupportedProvinceAndYear() {
        HouseholdInput p = new HouseholdInput(HouseholdInput.Mode.SINGLE, "BC", 2026, new BigDecimal("100"),
                false, new HouseholdInput.Assumptions(new BigDecimal("0.04"), new BigDecimal("0.20")),
                List.of(Fixtures.person(PersonId.A, "A", new BigDecimal("1"), "0", "0", false, "0", true)), null);
        assertEquals("UNSUPPORTED_PROVINCE", assertThrows(DomainException.class, () -> calc.calculate(p)).code());
        HouseholdInput y = new HouseholdInput(HouseholdInput.Mode.SINGLE, "ON", 2025, new BigDecimal("100"),
                false, new HouseholdInput.Assumptions(new BigDecimal("0.04"), new BigDecimal("0.20")),
                List.of(Fixtures.person(PersonId.A, "A", new BigDecimal("1"), "0", "0", false, "0", true)), null);
        assertEquals("UNSUPPORTED_TAX_YEAR", assertThrows(DomainException.class, () -> calc.calculate(y)).code());
    }

    @Test
    void budgetExceedsRoomWithUserAllocRejected() {
        HouseholdInput in = new HouseholdInput(
                HouseholdInput.Mode.SINGLE, "ON", 2026, new BigDecimal("1000"),
                false,
                new HouseholdInput.Assumptions(new BigDecimal("0.04"), new BigDecimal("0.20")),
                List.of(Fixtures.person(PersonId.A, "A", new BigDecimal("50000"), "100", "100", false, "0", true)),
                List.of(new ca.bracketwise.domain.allocation.PercentConverter.PercentLine(PersonId.A, Account.TFSA, new BigDecimal("100.00")))
        );
        assertEquals("BUDGET_EXCEEDS_TOTAL_ROOM", assertThrows(DomainException.class, () -> calc.calculate(in)).code());
    }

    @Test
    void modeledOnlyWhenBudgetExceedsRoom() {
        HouseholdInput in = new HouseholdInput(
                HouseholdInput.Mode.SINGLE, "ON", 2026, new BigDecimal("1000"),
                false,
                new HouseholdInput.Assumptions(new BigDecimal("0.04"), new BigDecimal("0.20")),
                List.of(Fixtures.person(PersonId.A, "A", new BigDecimal("50000"), "100", "100", false, "0", true)),
                null
        );
        CalculationResult r = calc.calculate(in);
        assertTrue(r.modeledAllocation().unallocated().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("ROOM_EXHAUSTED", r.modeledAllocation().unallocatedReason());
        assertNull(r.userAllocation());
    }

    @Test
    void zeroReturnProjectionEqualsContributionsTimesYears() {
        HouseholdInput in = new HouseholdInput(
                HouseholdInput.Mode.SINGLE, "ON", 2026, new BigDecimal("100"),
                false,
                new HouseholdInput.Assumptions(BigDecimal.ZERO, new BigDecimal("0.20")),
                List.of(Fixtures.person(PersonId.A, "A", new BigDecimal("50000"), "10000", "0", false, "0", true)),
                List.of(new ca.bracketwise.domain.allocation.PercentConverter.PercentLine(PersonId.A, Account.TFSA, new BigDecimal("100.00")))
        );
        CalculationResult r = calc.calculate(in);
        assertEquals("6000.00", Money.asString(r.projection().userDefined().householdPretaxByYear().get(4)));
    }
}
