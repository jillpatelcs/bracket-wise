package ca.bracketwise.domain.tax;

import ca.bracketwise.Fixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxCalculatorSyntheticTest {
    private final TaxCalculator tax = new TaxCalculator(Fixtures.SYNTHETIC);

    @Test
    void personAIncome54000MatchesFixture() {
        TaxComponents c = tax.calculate(new BigDecimal("54000"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("4700.00"), c.federal());
        assertEquals(new BigDecimal("2400.00"), c.ontario());
        assertEquals(new BigDecimal("0.00"), c.ontarioSurtax());
        assertEquals(new BigDecimal("300.00"), c.ontarioHealthPremium());
        assertEquals(new BigDecimal("7400.00"), c.total());
        assertEquals(new BigDecimal("0.1370"), tax.averageRate(new BigDecimal("54000"), c));
    }

    @Test
    void personBIncome45000MatchesFixture() {
        TaxComponents c = tax.calculate(new BigDecimal("45000"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("3400.00"), c.federal());
        assertEquals(new BigDecimal("1750.00"), c.ontario());
        assertEquals(new BigDecimal("5450.00"), c.total());
    }

    @Test
    void deductionsMatchFixtureSavings() {
        assertEquals(new BigDecimal("5990.00"), tax.total(new BigDecimal("54000"), new BigDecimal("5400")));
        assertEquals(new BigDecimal("1410.00"),
                tax.total(new BigDecimal("54000"), BigDecimal.ZERO)
                        .subtract(tax.total(new BigDecimal("54000"), new BigDecimal("5400"))));
        assertEquals(new BigDecimal("4910.00"), tax.total(new BigDecimal("45000"), new BigDecimal("3600")));
        assertEquals(new BigDecimal("6200.00"), tax.total(new BigDecimal("54000"), new BigDecimal("4000")));
        assertEquals(new BigDecimal("4850.00"), tax.total(new BigDecimal("45000"), new BigDecimal("4000")));
    }

    @Test
    void zeroIncomeIsZeroTax() {
        TaxComponents c = tax.calculate(BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), c.total());
    }

    @Test
    void deductionLargerThanIncomeFloorsTaxable() {
        TaxComponents c = tax.calculate(new BigDecimal("1000"), new BigDecimal("5000"));
        assertEquals(new BigDecimal("0.00"), c.total());
    }

    @Test
    void federalBracketBoundary50000() {
        TaxComponents below = tax.calculate(new BigDecimal("50000"), BigDecimal.ZERO);
        TaxComponents above = tax.calculate(new BigDecimal("50000.01"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("3900.00"), below.federal());
        assertEquals(new BigDecimal("3900.00"), above.federal());
    }

    @Test
    void surtaxBoundary() {
        TaxComponents at = tax.calculate(new BigDecimal("60000"), BigDecimal.ZERO);
        TaxComponents above = tax.calculate(new BigDecimal("60001"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), at.ontarioSurtax());
        assertEquals(new BigDecimal("3000.10"), above.ontario());
        assertEquals(new BigDecimal("0.02"), above.ontarioSurtax());
    }

    @Test
    void ohpBandEdge() {
        assertEquals(new BigDecimal("0.00"), tax.calculate(new BigDecimal("20000"), BigDecimal.ZERO).ontarioHealthPremium());
        assertEquals(new BigDecimal("300.00"), tax.calculate(new BigDecimal("20000.01"), BigDecimal.ZERO).ontarioHealthPremium());
    }

    @Test
    void savingsNeverExceedTaxBeforeDeductions() {
        BigDecimal income = new BigDecimal("54000");
        BigDecimal before = tax.total(income, BigDecimal.ZERO);
        BigDecimal savings = before.subtract(tax.total(income, income));
        org.junit.jupiter.api.Assertions.assertTrue(savings.compareTo(before) <= 0);
    }

    @Test
    void telescopingIncrementsEqualTotalSavings() {
        BigDecimal income = new BigDecimal("54000");
        BigDecimal d = BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < 40; i++) {
            BigDecimal delta = new BigDecimal("100.00");
            sum = sum.add(tax.estimatedTaxSavings(income, d, delta));
            d = d.add(delta);
        }
        BigDecimal total = tax.total(income, BigDecimal.ZERO).subtract(tax.total(income, d));
        assertEquals(total, sum);
    }
}
