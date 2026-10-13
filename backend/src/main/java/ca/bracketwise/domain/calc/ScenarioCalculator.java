package ca.bracketwise.domain.calc;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.DomainException;
import ca.bracketwise.domain.Money;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.allocation.Destination;
import ca.bracketwise.domain.allocation.ModeledAllocationResult;
import ca.bracketwise.domain.allocation.PercentConverter;
import ca.bracketwise.domain.allocation.RuleBasedAllocator;
import ca.bracketwise.domain.projection.ProjectionEngine;
import ca.bracketwise.domain.tax.TaxCalculator;
import ca.bracketwise.domain.tax.TaxComponents;
import ca.bracketwise.domain.tax.TaxParameters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ScenarioCalculator {
    public static final String MODELED_LABEL = "Tax-aware modeled allocation under these assumptions";

    private final TaxParameters taxParameters;
    private final PlanningDefaults defaults;
    private final TaxCalculator tax;
    private final RuleBasedAllocator allocator;
    private final ProjectionEngine projection = new ProjectionEngine();

    public ScenarioCalculator(TaxParameters taxParameters, PlanningDefaults defaults) {
        this.taxParameters = taxParameters;
        this.defaults = defaults;
        this.tax = new TaxCalculator(taxParameters);
        this.allocator = new RuleBasedAllocator(tax, defaults.allocationIncrement());
    }

    public TaxParameters taxParameters() {
        return taxParameters;
    }

    public PlanningDefaults defaults() {
        return defaults;
    }

    public CalculationResult calculate(HouseholdInput input) {
        HouseholdValidator.validate(input, defaults);
        BigDecimal budget = Money.twoExact(input.monthlySavings().multiply(new BigDecimal("12")));
        long budgetCents = Money.toCents(budget);
        long totalRoom = HouseholdValidator.totalRoomCents(input);

        boolean partYear = input.persons().stream().anyMatch(p -> !p.canadianTaxResidentForFullYear());
        List<CalculationResult.Warning> warnings = new ArrayList<>();
        warnings.add(new CalculationResult.Warning("FUTURE_ROOM_NOT_MODELED",
                "Years 2 to 5 repeat year-1 contributions; future contribution room is not modeled."));
        warnings.add(new CalculationResult.Warning("SIMPLIFIED_ESTIMATE",
                "Simplified estimated income tax; excludes CPP, EI, and most credits."));

        CalculationResult.ResultAssumptions assumptions = assumptions(input, budget);
        List<CalculationResult.AvailableRoom> rooms = input.persons().stream()
                .map(p -> new CalculationResult.AvailableRoom(
                        p.id(), p.availableTfsaRoom(), p.availableRrspDeductionLimit(),
                        p.availableFhsaParticipationRoom(), p.fhsaEligible()))
                .toList();

        Map<PersonId, Map<Account, Long>> roomCents = roomCents(input);

        if (partYear) {
            warnings.add(0, new CalculationResult.Warning("TAX_OPTIMIZATION_DISABLED_PART_YEAR",
                    "Tax-based estimates and the tax-aware modeled allocation are not computed because full-year Canadian tax residency was not confirmed for every person."));
            CalculationResult.AllocationView userView = null;
            ProjectionEngine.ScenarioProjection userProj = null;
            if (input.userAllocation() != null && !input.userAllocation().isEmpty()) {
                if (budgetCents > totalRoom) {
                    throw new DomainException("BUDGET_EXCEEDS_TOTAL_ROOM", "monthlySavings",
                            "The annual savings budget exceeds total entered room. Omit the user-defined allocation to run the tax-aware allocation only.");
                }
                List<PercentConverter.ConvertedLine> converted = PercentConverter.convert(budgetCents, input.userAllocation(), roomCents);
                userView = userView(converted, input, budget);
                userProj = projection.project(toMap(converted), input.assumptions().expectedAnnualReturn(),
                        input.assumptions().rrspWithdrawalTaxRate(), input.fhsaFirstHomePlan(), Money.ZERO);
            }
            return new CalculationResult(
                    taxParameters.version(), warnings, assumptions, null, rooms, userView, null, null,
                    new CalculationResult.ProjectionView(ProjectionEngine.START_YEAR, ProjectionEngine.HORIZON, userProj, null),
                    new CalculationResult.ExplanationView(List.of(), List.of())
            );
        }

        if (input.userAllocation() != null && !input.userAllocation().isEmpty() && budgetCents > totalRoom) {
            throw new DomainException("BUDGET_EXCEEDS_TOTAL_ROOM", "monthlySavings",
                    "The annual savings budget exceeds total entered room. Omit the user-defined allocation to run the tax-aware allocation only.");
        }

        List<CalculationResult.PersonTax> personTaxes = input.persons().stream()
                .map(p -> {
                    TaxComponents c = tax.calculate(p.estimatedEmploymentIncomeFor2026(), BigDecimal.ZERO);
                    return new CalculationResult.PersonTax(p.id(), c, tax.averageRate(p.estimatedEmploymentIncomeFor2026(), c));
                })
                .toList();

        Map<PersonId, RuleBasedAllocator.PersonState> states = input.persons().stream()
                .collect(Collectors.toMap(HouseholdInput.PersonInput::id, p -> new RuleBasedAllocator.PersonState(
                        p.id(),
                        p.estimatedEmploymentIncomeFor2026(),
                        p.availableTfsaRoom(),
                        p.availableRrspDeductionLimit(),
                        p.fhsaEligible(),
                        p.availableFhsaParticipationRoom()
                )));

        ModeledAllocationResult modeled = allocator.allocate(
                budgetCents, states, input.fhsaFirstHomePlan(), input.assumptions().rrspWithdrawalTaxRate());

        CalculationResult.AllocationView userView = null;
        Map<Destination, BigDecimal> userAmounts = Map.of();
        if (input.userAllocation() != null && !input.userAllocation().isEmpty()) {
            List<PercentConverter.ConvertedLine> converted = PercentConverter.convert(budgetCents, input.userAllocation(), roomCents);
            userView = userView(converted, input, budget);
            userAmounts = toMap(converted);
        }

        CalculationResult.ScenarioSavings userSavings = input.userAllocation() == null || input.userAllocation().isEmpty()
                ? null
                : savingsFor(input, userAmounts);
        CalculationResult.ScenarioSavings modeledSavings = savingsFor(input, modeled.amounts());

        ProjectionEngine.ScenarioProjection userProj = userView == null ? null : projection.project(
                userAmounts,
                input.assumptions().expectedAnnualReturn(),
                input.assumptions().rrspWithdrawalTaxRate(),
                input.fhsaFirstHomePlan(),
                userSavings.annualTotal()
        );
        ProjectionEngine.ScenarioProjection modeledProj = projection.project(
                modeled.amounts(),
                input.assumptions().expectedAnnualReturn(),
                input.assumptions().rrspWithdrawalTaxRate(),
                input.fhsaFirstHomePlan(),
                modeledSavings.annualTotal()
        );

        CalculationResult.AllocationView modeledView = new CalculationResult.AllocationView(
                MODELED_LABEL,
                modeled.amounts().entrySet().stream()
                        .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                        .sorted((a, b) -> Integer.compare(a.getKey().order(), b.getKey().order()))
                        .map(e -> new CalculationResult.AllocationLine(e.getKey().person(), e.getKey().account(), null, e.getValue()))
                        .toList(),
                modeled.unusedRoom(),
                modeled.unallocated(),
                modeled.unallocatedReason()
        );

        List<String> differs = whatDiffers(userView, modeledView, userSavings, modeledSavings, userProj, modeledProj,
                input.assumptions().rrspWithdrawalTaxRate());

        return new CalculationResult(
                taxParameters.version(),
                warnings,
                assumptions,
                personTaxes,
                rooms,
                userView,
                modeledView,
                new CalculationResult.TaxSavingsView(userSavings, modeledSavings),
                new CalculationResult.ProjectionView(ProjectionEngine.START_YEAR, ProjectionEngine.HORIZON, userProj, modeledProj),
                new CalculationResult.ExplanationView(modeled.steps(), differs)
        );
    }

    private CalculationResult.ResultAssumptions assumptions(HouseholdInput input, BigDecimal budget) {
        return new CalculationResult.ResultAssumptions(
                input.assumptions().expectedAnnualReturn(),
                input.assumptions().rrspWithdrawalTaxRate(),
                defaults.horizonYears(),
                "START_OF_YEAR",
                input.fhsaFirstHomePlan(),
                input.fhsaFirstHomePlan() ? "TAX_FREE_QUALIFYING_WITHDRAWAL" : "TAXED_LIKE_RRSP",
                defaults.allocationIncrement(),
                budget,
                "RULE_BASED_V1"
        );
    }

    private Map<PersonId, Map<Account, Long>> roomCents(HouseholdInput input) {
        Map<PersonId, Map<Account, Long>> map = new EnumMap<>(PersonId.class);
        for (HouseholdInput.PersonInput p : input.persons()) {
            Map<Account, Long> rooms = new EnumMap<>(Account.class);
            rooms.put(Account.TFSA, Money.toCents(p.availableTfsaRoom()));
            rooms.put(Account.RRSP, Money.toCents(p.availableRrspDeductionLimit()));
            if (p.fhsaEligible()) {
                rooms.put(Account.FHSA, Money.toCents(p.availableFhsaParticipationRoom()));
            }
            map.put(p.id(), rooms);
        }
        return map;
    }

    private CalculationResult.AllocationView userView(
            List<PercentConverter.ConvertedLine> converted,
            HouseholdInput input,
            BigDecimal budget
    ) {
        Map<PersonId, Map<Account, BigDecimal>> unused = new EnumMap<>(PersonId.class);
        for (HouseholdInput.PersonInput p : input.persons()) {
            Map<Account, BigDecimal> u = new EnumMap<>(Account.class);
            u.put(Account.TFSA, p.availableTfsaRoom());
            u.put(Account.RRSP, p.availableRrspDeductionLimit());
            u.put(Account.FHSA, p.availableFhsaParticipationRoom());
            unused.put(p.id(), u);
        }
        List<CalculationResult.AllocationLine> lines = new ArrayList<>();
        for (PercentConverter.ConvertedLine line : converted) {
            lines.add(new CalculationResult.AllocationLine(line.person(), line.account(), line.percent(), line.amount()));
            Map<Account, BigDecimal> u = unused.get(line.person());
            u.put(line.account(), u.get(line.account()).subtract(line.amount()));
        }
        BigDecimal allocated = converted.stream().map(PercentConverter.ConvertedLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CalculationResult.AllocationView(null, lines, unused, budget.subtract(allocated), null);
    }

    private Map<Destination, BigDecimal> toMap(List<PercentConverter.ConvertedLine> converted) {
        Map<Destination, BigDecimal> map = new EnumMap<>(Destination.class);
        for (PercentConverter.ConvertedLine line : converted) {
            map.put(Destination.of(line.person(), line.account()), line.amount());
        }
        return map;
    }

    private CalculationResult.ScenarioSavings savingsFor(HouseholdInput input, Map<Destination, BigDecimal> amounts) {
        List<CalculationResult.PersonSavings> list = new ArrayList<>();
        BigDecimal annual = BigDecimal.ZERO;
        for (HouseholdInput.PersonInput p : input.persons()) {
            BigDecimal deductions = amounts.getOrDefault(Destination.of(p.id(), Account.RRSP), BigDecimal.ZERO)
                    .add(amounts.getOrDefault(Destination.of(p.id(), Account.FHSA), BigDecimal.ZERO));
            TaxComponents before = tax.calculate(p.estimatedEmploymentIncomeFor2026(), BigDecimal.ZERO);
            TaxComponents after = tax.calculate(p.estimatedEmploymentIncomeFor2026(), deductions);
            BigDecimal savings = before.total().subtract(after.total());
            list.add(new CalculationResult.PersonSavings(p.id(), deductions, after.total(), savings));
            annual = annual.add(savings);
        }
        return new CalculationResult.ScenarioSavings(list, annual, annual.multiply(new BigDecimal("5")));
    }

    private List<String> whatDiffers(
            CalculationResult.AllocationView user,
            CalculationResult.AllocationView modeled,
            CalculationResult.ScenarioSavings userSavings,
            CalculationResult.ScenarioSavings modeledSavings,
            ProjectionEngine.ScenarioProjection userProj,
            ProjectionEngine.ScenarioProjection modeledProj,
            BigDecimal w
    ) {
        List<String> lines = new ArrayList<>();
        if (user == null) {
            lines.add("Only the tax-aware modeled allocation was produced because a user-defined allocation was not provided.");
            return lines;
        }
        Map<Account, BigDecimal> userBy = byAccount(user);
        Map<Account, BigDecimal> modeledBy = byAccount(modeled);
        lines.add("Contributions by account type (per year): RRSP $" + Money.asString(userBy.get(Account.RRSP))
                + " (yours) vs. $" + Money.asString(modeledBy.get(Account.RRSP))
                + " (modeled); FHSA $" + Money.asString(userBy.get(Account.FHSA))
                + " vs. $" + Money.asString(modeledBy.get(Account.FHSA))
                + "; TFSA $" + Money.asString(userBy.get(Account.TFSA))
                + " vs. $" + Money.asString(modeledBy.get(Account.TFSA)) + ".");
        if (userSavings != null && modeledSavings != null) {
            lines.add("Estimated current-year tax savings: $" + Money.asString(userSavings.annualTotal())
                    + " (yours) vs. $" + Money.asString(modeledSavings.annualTotal()) + " (modeled).");
        }
        if (userProj != null && modeledProj != null) {
            BigDecimal diff = modeledProj.estimatedAfterTaxAccountValue().subtract(userProj.estimatedAfterTaxAccountValue()).abs();
            lines.add("Estimated after-tax account value at year 5: $"
                    + Money.asString(userProj.estimatedAfterTaxAccountValue())
                    + " (yours) vs. $" + Money.asString(modeledProj.estimatedAfterTaxAccountValue())
                    + " (modeled), a difference of $" + Money.asString(diff)
                    + " under the " + w.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    + "% withdrawal-tax assumption.");
        }
        return lines;
    }

    private Map<Account, BigDecimal> byAccount(CalculationResult.AllocationView view) {
        Map<Account, BigDecimal> map = new LinkedHashMap<>();
        map.put(Account.TFSA, Money.ZERO);
        map.put(Account.RRSP, Money.ZERO);
        map.put(Account.FHSA, Money.ZERO);
        for (CalculationResult.AllocationLine line : view.allocations()) {
            map.merge(line.account(), line.amount(), BigDecimal::add);
        }
        return map;
    }
}
