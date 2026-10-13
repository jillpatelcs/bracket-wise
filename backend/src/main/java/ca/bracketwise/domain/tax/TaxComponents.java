package ca.bracketwise.domain.tax;

import ca.bracketwise.domain.Money;

import java.math.BigDecimal;

public record TaxComponents(
        BigDecimal federal,
        BigDecimal ontario,
        BigDecimal ontarioSurtax,
        BigDecimal ontarioHealthPremium,
        BigDecimal total
) {
    public static TaxComponents of(BigDecimal federal, BigDecimal ontario, BigDecimal surtax, BigDecimal ohp) {
        BigDecimal f = Money.two(federal);
        BigDecimal o = Money.two(ontario);
        BigDecimal s = Money.two(surtax);
        BigDecimal h = Money.two(ohp);
        return new TaxComponents(f, o, s, h, f.add(o).add(s).add(h));
    }
}
