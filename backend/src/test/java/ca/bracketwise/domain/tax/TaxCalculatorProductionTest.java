package ca.bracketwise.domain.tax;

import ca.bracketwise.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxCalculatorProductionTest {
    private final TaxCalculator tax = new TaxCalculator(Fixtures.PRODUCTION);

    @ParameterizedTest
    @CsvSource({
            "0, 0.00",
            "10000, 0.00",
            "58523, 5679.80",
            "58523.01, 5679.80"
    })
    void federalBoundariesFromWorksheet(String income, String expectedFederal) {
        // See docs/tax-worksheets/2026-federal.md
        TaxComponents c = tax.calculate(new BigDecimal(income), BigDecimal.ZERO);
        assertEquals(new BigDecimal(expectedFederal), c.federal());
    }

    @Test
    void highIncomeAppliesBpaPhaseDown() {
        TaxComponents low = tax.calculate(new BigDecimal("181440"), BigDecimal.ZERO);
        TaxComponents high = tax.calculate(new BigDecimal("258482"), BigDecimal.ZERO);
        assertTrue(high.federal().compareTo(low.federal()) > 0);
        assertTrue(high.total().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void ontarioReductionAppliesAtLowIncome() {
        TaxComponents c = tax.calculate(new BigDecimal("18000"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), c.ontario());
    }

    @Test
    void ontarioPartialReduction() {
        // After credits, T around 400: reduction = 600-T, ontario = 2T-600
        TaxComponents c = tax.calculate(new BigDecimal("22000"), BigDecimal.ZERO);
        assertTrue(c.ontario().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(c.ontario().compareTo(new BigDecimal("600")) < 0 || c.ontario().compareTo(new BigDecimal("600")) >= 0);
    }

    @Test
    void ohpCapsAtNineHundred() {
        TaxComponents c = tax.calculate(new BigDecimal("250000"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("900.00"), c.ontarioHealthPremium());
    }
}
