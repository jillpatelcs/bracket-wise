package ca.bracketwise.domain.projection;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.Money;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.allocation.Destination;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ProjectionEngine {
    public static final int HORIZON = 5;
    public static final int START_YEAR = 2026;

    public record YearFiveBalance(PersonId person, Account account, BigDecimal pretax, BigDecimal estimatedAfterTax) {}

    public record ScenarioProjection(
            List<BigDecimal> householdPretaxByYear,
            Map<PersonId, Map<Account, List<BigDecimal>>> balancesByYear,
            List<YearFiveBalance> yearFiveBalances,
            BigDecimal estimatedAfterTaxAccountValue,
            BigDecimal taxSavingsIfSaved
    ) {}

    public ScenarioProjection project(
            Map<Destination, BigDecimal> annualContributions,
            BigDecimal r,
            BigDecimal withdrawalRate,
            boolean fhsaFirstHomePlan,
            BigDecimal annualTaxSavings
    ) {
        Map<Destination, BigDecimal> balances = new EnumMap<>(Destination.class);
        Map<PersonId, Map<Account, List<BigDecimal>>> byYear = new EnumMap<>(PersonId.class);
        List<BigDecimal> household = new ArrayList<>();

        for (int year = 1; year <= HORIZON; year++) {
            BigDecimal householdTotal = BigDecimal.ZERO;
            for (Map.Entry<Destination, BigDecimal> e : annualContributions.entrySet()) {
                if (e.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal prior = balances.getOrDefault(e.getKey(), BigDecimal.ZERO);
                BigDecimal grown = prior.add(e.getValue()).multiply(BigDecimal.ONE.add(r));
                BigDecimal rounded = Money.two(grown);
                balances.put(e.getKey(), rounded);
                householdTotal = householdTotal.add(rounded);
                byYear.computeIfAbsent(e.getKey().person(), k -> new EnumMap<>(Account.class))
                        .computeIfAbsent(e.getKey().account(), k -> new ArrayList<>())
                        .add(rounded);
            }
            household.add(householdTotal);
        }

        List<YearFiveBalance> yearFive = new ArrayList<>();
        BigDecimal afterTaxTotal = BigDecimal.ZERO;
        for (Destination dest : Destination.canonical()) {
            BigDecimal pretax = balances.get(dest);
            if (pretax == null) {
                continue;
            }
            BigDecimal after = afterTax(dest.account(), pretax, withdrawalRate, fhsaFirstHomePlan);
            yearFive.add(new YearFiveBalance(dest.person(), dest.account(), pretax, after));
            afterTaxTotal = afterTaxTotal.add(after);
        }

        BigDecimal taxSavingsIfSaved = taxSavingsIfSaved(annualTaxSavings, r);
        return new ScenarioProjection(household, byYear, yearFive, afterTaxTotal, taxSavingsIfSaved);
    }

    public static BigDecimal afterTax(Account account, BigDecimal pretax, BigDecimal w, boolean fhsaFirstHomePlan) {
        if (account == Account.TFSA) {
            return pretax;
        }
        if (account == Account.FHSA && fhsaFirstHomePlan) {
            return pretax;
        }
        return Money.two(pretax.multiply(BigDecimal.ONE.subtract(w)));
    }

    /**
     * Each year's savings is saved at year-end and grown at r for the remaining years, rounded annually.
     * Year 1 savings grows 4 years; year 5 savings is not grown.
     */
    static BigDecimal taxSavingsIfSaved(BigDecimal annualSavings, BigDecimal r) {
        BigDecimal total = BigDecimal.ZERO;
        for (int year = 1; year <= HORIZON; year++) {
            BigDecimal value = annualSavings;
            int remaining = HORIZON - year;
            for (int g = 0; g < remaining; g++) {
                value = Money.two(value.multiply(BigDecimal.ONE.add(r)));
            }
            total = total.add(value);
        }
        return Money.two(total);
    }
}
