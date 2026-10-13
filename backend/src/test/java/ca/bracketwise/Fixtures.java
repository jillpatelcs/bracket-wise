package ca.bracketwise;

import ca.bracketwise.config.TaxConfigLoader;
import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.allocation.PercentConverter;
import ca.bracketwise.domain.calc.HouseholdInput;
import ca.bracketwise.domain.calc.PlanningDefaults;
import ca.bracketwise.domain.tax.TaxParameters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Fixtures {
    public static final TaxConfigLoader LOADER = new TaxConfigLoader();
    public static final TaxParameters SYNTHETIC = LOADER.loadTax("tax/synthetic-fixture-1.json");
    public static final TaxParameters PRODUCTION = LOADER.loadTax("tax/tax-2026.json");
    public static final PlanningDefaults DEFAULTS = LOADER.loadDefaults("tax/planning-defaults.json");

    private Fixtures() {}

    public static HouseholdInput exampleCouple() {
        return exampleCouple(true, true);
    }

    public static HouseholdInput exampleCouple(boolean fullYearB, boolean withUserAlloc) {
        HouseholdInput.PersonInput a = person(PersonId.A, "Person A", new BigDecimal("54000"),
                "6000", "8000", true, "3000", true);
        HouseholdInput.PersonInput b = person(PersonId.B, "Person B", new BigDecimal("45000"),
                "9000", "5000", true, "4000", fullYearB);
        List<PercentConverter.PercentLine> alloc = withUserAlloc ? List.of(
                new PercentConverter.PercentLine(PersonId.A, Account.TFSA, new BigDecimal("20.00")),
                new PercentConverter.PercentLine(PersonId.A, Account.RRSP, new BigDecimal("20.00")),
                new PercentConverter.PercentLine(PersonId.A, Account.FHSA, new BigDecimal("10.00")),
                new PercentConverter.PercentLine(PersonId.B, Account.TFSA, new BigDecimal("30.00")),
                new PercentConverter.PercentLine(PersonId.B, Account.RRSP, new BigDecimal("10.00")),
                new PercentConverter.PercentLine(PersonId.B, Account.FHSA, new BigDecimal("10.00"))
        ) : null;
        return new HouseholdInput(
                HouseholdInput.Mode.COUPLE,
                "ON",
                2026,
                new BigDecimal("1500"),
                true,
                new HouseholdInput.Assumptions(new BigDecimal("0.05"), new BigDecimal("0.20")),
                List.of(a, b),
                alloc
        );
    }

    public static HouseholdInput.PersonInput person(
            PersonId id, String label, BigDecimal income,
            String tfsa, String rrsp, boolean fhsa, String fhsaRoom, boolean fullYear
    ) {
        return new HouseholdInput.PersonInput(
                id, label, true, LocalDate.parse("2024-09-01"), null, null, fullYear,
                income, new BigDecimal(tfsa), new BigDecimal(rrsp), fhsa, new BigDecimal(fhsaRoom)
        );
    }
}
