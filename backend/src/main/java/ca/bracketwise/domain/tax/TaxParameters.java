package ca.bracketwise.domain.tax;

import java.math.BigDecimal;
import java.util.List;

public record TaxParameters(
        int effectiveTaxYear,
        String version,
        String status,
        String statusNote,
        String retrievedOn,
        String source,
        Federal federal,
        Ontario ontario
) {
    public record Federal(
            String source,
            String retrievedOn,
            BigDecimal lowestRate,
            List<Bracket> brackets,
            BigDecimal basicPersonalAmountMax,
            BigDecimal basicPersonalAmountMin,
            BigDecimal bpaPhaseOutStart,
            BigDecimal bpaPhaseOutEnd,
            BigDecimal bpaSupplement,
            BigDecimal canadaEmploymentAmount,
            String creditsSource,
            String creditsRetrievedOn,
            String bpaPhaseDownSource
    ) {}

    public record Ontario(
            String source,
            String retrievedOn,
            BigDecimal lowestRate,
            List<Bracket> brackets,
            BigDecimal basicPersonalAmount,
            String basicPersonalAmountSource,
            BigDecimal surtaxThreshold1,
            BigDecimal surtaxRate1,
            BigDecimal surtaxThreshold2,
            BigDecimal surtaxRate2,
            String surtaxSource,
            BigDecimal taxReductionBasicAmount,
            String taxReductionSource,
            String ohpIncomeBasis,
            List<OhpBand> ohpBands,
            String ohpSource
    ) {}

    public record Bracket(BigDecimal upTo, BigDecimal rate) {}

    public record OhpBand(
            BigDecimal upTo,
            BigDecimal flat,
            BigDecimal base,
            BigDecimal rate,
            BigDecimal over,
            BigDecimal cap
    ) {}
}
