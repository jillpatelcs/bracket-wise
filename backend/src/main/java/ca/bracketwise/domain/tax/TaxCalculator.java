package ca.bracketwise.domain.tax;

import ca.bracketwise.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class TaxCalculator {
    private final TaxParameters params;

    public TaxCalculator(TaxParameters params) {
        this.params = params;
    }

    public TaxComponents calculate(BigDecimal income, BigDecimal deductions) {
        BigDecimal taxable = income.subtract(deductions).max(BigDecimal.ZERO);
        BigDecimal federal = federalTax(taxable, income);
        BigDecimal ontarioAfterCredits = ontarioTaxAfterCredits(taxable);
        BigDecimal reduction = ontarioTaxReduction(ontarioAfterCredits);
        BigDecimal ontario = ontarioAfterCredits.subtract(reduction).max(BigDecimal.ZERO);
        BigDecimal ontarioRounded = Money.two(ontario);
        BigDecimal surtax = ontarioSurtax(ontarioRounded);
        BigDecimal ohp = ontarioHealthPremium(taxable);
        return TaxComponents.of(federal, ontario, surtax, ohp);
    }

    public BigDecimal total(BigDecimal income, BigDecimal deductions) {
        return calculate(income, deductions).total();
    }

    public BigDecimal estimatedTaxSavings(BigDecimal income, BigDecimal currentDeduction, BigDecimal delta) {
        return total(income, currentDeduction).subtract(total(income, currentDeduction.add(delta)));
    }

    public BigDecimal averageRate(BigDecimal income, TaxComponents tax) {
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return Money.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return tax.total().divide(income, 4, RoundingMode.HALF_UP);
    }

    BigDecimal federalTax(BigDecimal taxable, BigDecimal incomeForBpa) {
        BigDecimal gross = progressive(taxable, params.federal().brackets());
        BigDecimal bpa = basicPersonalAmount(incomeForBpa);
        BigDecimal employment = params.federal().canadaEmploymentAmount().min(incomeForBpa.max(BigDecimal.ZERO));
        BigDecimal credits = bpa.add(employment).multiply(params.federal().lowestRate());
        return gross.subtract(credits).max(BigDecimal.ZERO);
    }

    BigDecimal basicPersonalAmount(BigDecimal income) {
        TaxParameters.Federal f = params.federal();
        if (income.compareTo(f.bpaPhaseOutEnd()) >= 0) {
            return f.basicPersonalAmountMin();
        }
        if (income.compareTo(f.bpaPhaseOutStart()) <= 0) {
            return f.basicPersonalAmountMax();
        }
        BigDecimal span = f.bpaPhaseOutEnd().subtract(f.bpaPhaseOutStart());
        BigDecimal over = income.subtract(f.bpaPhaseOutStart());
        BigDecimal remaining = span.subtract(over);
        BigDecimal supplement = f.bpaSupplement().multiply(remaining).divide(span, 16, RoundingMode.DOWN);
        // exact fraction; BPA is an intermediate amount, not a rounding boundary
        return f.basicPersonalAmountMin().add(supplement);
    }

    BigDecimal ontarioTaxAfterCredits(BigDecimal taxable) {
        BigDecimal gross = progressive(taxable, params.ontario().brackets());
        BigDecimal credit = params.ontario().basicPersonalAmount().multiply(params.ontario().lowestRate());
        return gross.subtract(credit).max(BigDecimal.ZERO);
    }

    BigDecimal ontarioTaxReduction(BigDecimal ontarioAfterCredits) {
        BigDecimal twice = params.ontario().taxReductionBasicAmount().multiply(new BigDecimal("2"));
        BigDecimal raw = twice.subtract(ontarioAfterCredits);
        if (raw.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return raw.min(ontarioAfterCredits);
    }

    BigDecimal ontarioSurtax(BigDecimal ontarioTaxAfterReduction) {
        TaxParameters.Ontario o = params.ontario();
        BigDecimal t = ontarioTaxAfterReduction;
        if (t.compareTo(o.surtaxThreshold1()) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal first = t.subtract(o.surtaxThreshold1()).multiply(o.surtaxRate1());
        if (t.compareTo(o.surtaxThreshold2()) <= 0) {
            return first;
        }
        BigDecimal second = t.subtract(o.surtaxThreshold2()).multiply(o.surtaxRate2());
        return first.add(second);
    }

    BigDecimal ontarioHealthPremium(BigDecimal taxable) {
        List<TaxParameters.OhpBand> bands = params.ontario().ohpBands();
        for (TaxParameters.OhpBand band : bands) {
            if (band.upTo() == null || taxable.compareTo(band.upTo()) <= 0) {
                if (band.flat() != null) {
                    return band.flat();
                }
                BigDecimal computed = band.base().add(taxable.subtract(band.over()).multiply(band.rate()));
                return computed.min(band.cap()).max(BigDecimal.ZERO);
            }
        }
        return BigDecimal.ZERO;
    }

    static BigDecimal progressive(BigDecimal taxable, List<TaxParameters.Bracket> brackets) {
        BigDecimal remaining = taxable;
        BigDecimal previous = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (TaxParameters.Bracket bracket : brackets) {
            BigDecimal ceiling = bracket.upTo() == null ? remaining.add(previous) : bracket.upTo();
            BigDecimal slice = remaining.min(ceiling.subtract(previous).max(BigDecimal.ZERO));
            tax = tax.add(slice.multiply(bracket.rate()));
            remaining = remaining.subtract(slice);
            previous = ceiling;
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
        return tax;
    }
}
