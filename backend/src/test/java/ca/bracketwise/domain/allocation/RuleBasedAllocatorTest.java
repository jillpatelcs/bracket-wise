package ca.bracketwise.domain.allocation;

import ca.bracketwise.Fixtures;
import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.Money;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.calc.ScenarioCalculator;
import ca.bracketwise.domain.tax.TaxCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedAllocatorTest {
    private final TaxCalculator tax = new TaxCalculator(Fixtures.SYNTHETIC);
    private final RuleBasedAllocator allocator = new RuleBasedAllocator(tax, new BigDecimal("100.00"));

    @Test
    void exampleCoupleFollowsR1R2R3() {
        ModeledAllocationResult r = allocator.allocate(1_800_000, couple(true), true, new BigDecimal("0.20"));
        assertEquals("3000.00", Money.asString(r.amounts().get(Destination.A_FHSA)));
        assertEquals("4000.00", Money.asString(r.amounts().get(Destination.B_FHSA)));
        assertEquals("1000.00", Money.asString(r.amounts().get(Destination.A_RRSP)));
        assertEquals("6000.00", Money.asString(r.amounts().get(Destination.A_TFSA)));
        assertEquals("4000.00", Money.asString(r.amounts().get(Destination.B_TFSA)));
        assertEquals("0.00", Money.asString(r.unallocated()));
        assertEquals(AllocationRule.R1_FHSA_FIRST, r.steps().get(0).rule());
        assertEquals(AllocationRule.R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE, r.steps().get(2).rule());
        assertNotNull(r.steps().get(2).nextIncrement());
        assertEquals("0.1500", r.steps().get(2).nextIncrement().savingsRate().toPlainString());
    }

    @Test
    void rateEqualToWGoesToTfsaNotR2() {
        RuleBasedAllocator.PersonState a = new RuleBasedAllocator.PersonState(
                PersonId.A, new BigDecimal("45000"), new BigDecimal("9000"),
                new BigDecimal("5000"), false, BigDecimal.ZERO);
        ModeledAllocationResult r = allocator.allocate(500_000, Map.of(PersonId.A, a), false, new BigDecimal("0.15"));
        assertFalse(r.amounts().containsKey(Destination.A_RRSP)
                && r.amounts().get(Destination.A_RRSP).compareTo(BigDecimal.ZERO) > 0
                && r.steps().stream().anyMatch(s -> s.rule() == AllocationRule.R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE));
        assertTrue(r.amounts().getOrDefault(Destination.A_TFSA, BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void fhsaTreatedLikeRrspWhenCheckboxFalse() {
        ModeledAllocationResult r = allocator.allocate(1_800_000, couple(true), false, new BigDecimal("0.20"));
        assertTrue(r.steps().stream().noneMatch(s -> s.rule() == AllocationRule.R1_FHSA_FIRST));
    }

    @Test
    void ineligibleFhsaExcluded() {
        ModeledAllocationResult r = allocator.allocate(1_800_000, couple(false), true, new BigDecimal("0.20"));
        assertFalse(r.amounts().containsKey(Destination.A_FHSA));
        assertFalse(r.amounts().containsKey(Destination.B_FHSA));
    }

    @Test
    void partialIncrementWorkedExample() {
        RuleBasedAllocator.PersonState a = new RuleBasedAllocator.PersonState(
                PersonId.A, new BigDecimal("54000"), BigDecimal.ZERO,
                new BigDecimal("4250.50"), false, BigDecimal.ZERO);
        ModeledAllocationResult r = allocator.allocate(425_050, Map.of(PersonId.A, a), false, new BigDecimal("0.10"));
        ExplanationStep step = r.steps().get(0);
        assertEquals("4250.50", Money.asString(step.amount()));
        assertEquals(43, step.incrementCount());
        assertEquals("1237.57", Money.asString(step.estimatedTaxSavings()));
        assertEquals(new BigDecimal("0.2912"), step.effectiveSavingsRate());
        assertEquals(new BigDecimal("0.1499"), step.savingsRateMin());
        assertEquals(new BigDecimal("0.3000"), step.savingsRateMax());
        assertNull(step.nextIncrement());
    }

    @Test
    void raisingWNeverIncreasesR2() {
        long r2Low = r2Cents(new BigDecimal("0.10"));
        long r2High = r2Cents(new BigDecimal("0.40"));
        assertTrue(r2High <= r2Low);
    }

    @Test
    void neverExceedsBudgetOrRoom() {
        ModeledAllocationResult r = allocator.allocate(1_800_000, couple(true), true, new BigDecimal("0.20"));
        long sum = r.amounts().values().stream().mapToLong(Money::toCents).sum();
        assertEquals(1_800_000, sum + Money.toCents(r.unallocated()));
        assertTrue(r.amounts().getOrDefault(Destination.A_TFSA, BigDecimal.ZERO).compareTo(new BigDecimal("6000")) <= 0);
    }

    @Test
    void reconciliationInvariants() {
        var calc = new ScenarioCalculator(Fixtures.SYNTHETIC, Fixtures.DEFAULTS);
        var result = calc.calculate(ca.bracketwise.Fixtures.exampleCouple());
        for (var step : result.explanation().steps()) {
            if (step.deductible()) {
                assertTrue(step.savingsRateMin().compareTo(step.effectiveSavingsRate()) <= 0);
                assertTrue(step.effectiveSavingsRate().compareTo(step.savingsRateMax()) <= 0);
            }
        }
        BigDecimal aSteps = result.explanation().steps().stream()
                .filter(s -> s.person() == PersonId.A)
                .map(ExplanationStep::estimatedTaxSavings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal aModeled = result.estimatedTaxSavings().modeled().perPerson().stream()
                .filter(p -> p.person() == PersonId.A)
                .findFirst().orElseThrow().estimatedTaxSavings();
        assertEquals(aModeled, aSteps);
    }

    private long r2Cents(BigDecimal w) {
        ModeledAllocationResult r = allocator.allocate(1_800_000, couple(true), false, w);
        return r.steps().stream()
                .filter(s -> s.rule() == AllocationRule.R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE)
                .mapToLong(s -> Money.toCents(s.amount()))
                .sum();
    }

    private Map<PersonId, RuleBasedAllocator.PersonState> couple(boolean fhsa) {
        Map<PersonId, RuleBasedAllocator.PersonState> map = new EnumMap<>(PersonId.class);
        map.put(PersonId.A, new RuleBasedAllocator.PersonState(
                PersonId.A, new BigDecimal("54000"), new BigDecimal("6000"),
                new BigDecimal("8000"), fhsa, fhsa ? new BigDecimal("3000") : BigDecimal.ZERO));
        map.put(PersonId.B, new RuleBasedAllocator.PersonState(
                PersonId.B, new BigDecimal("45000"), new BigDecimal("9000"),
                new BigDecimal("5000"), fhsa, fhsa ? new BigDecimal("4000") : BigDecimal.ZERO));
        return map;
    }
}
