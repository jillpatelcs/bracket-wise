package ca.bracketwise.domain.calc;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.allocation.ExplanationStep;
import ca.bracketwise.domain.allocation.PercentConverter;
import ca.bracketwise.domain.projection.ProjectionEngine;
import ca.bracketwise.domain.tax.TaxComponents;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CalculationResult(
        String configVersion,
        List<Warning> warnings,
        ResultAssumptions assumptions,
        List<PersonTax> perPersonTax,
        List<AvailableRoom> availableRoom,
        AllocationView userAllocation,
        AllocationView modeledAllocation,
        TaxSavingsView estimatedTaxSavings,
        ProjectionView projection,
        ExplanationView explanation
) {
    public record Warning(String code, String message) {}

    public record ResultAssumptions(
            BigDecimal expectedAnnualReturn,
            BigDecimal rrspWithdrawalTaxRate,
            int horizonYears,
            String contributionTiming,
            boolean fhsaFirstHomePlan,
            String fhsaTreatment,
            BigDecimal allocationIncrement,
            BigDecimal annualBudget,
            String allocationMethod
    ) {}

    public record PersonTax(PersonId person, TaxComponents components, BigDecimal averageEstimatedTaxRate) {}

    public record AvailableRoom(
            PersonId person,
            BigDecimal tfsa,
            BigDecimal rrspDeductionLimit,
            BigDecimal fhsa,
            boolean fhsaEligible
    ) {}

    public record AllocationLine(PersonId person, Account account, BigDecimal percent, BigDecimal amount) {}

    public record AllocationView(
            String label,
            List<AllocationLine> allocations,
            Map<PersonId, Map<Account, BigDecimal>> unusedRoom,
            BigDecimal unallocated,
            String unallocatedReason
    ) {}

    public record PersonSavings(
            PersonId person,
            BigDecimal totalDeductions,
            BigDecimal taxAfterDeductions,
            BigDecimal estimatedTaxSavings
    ) {}

    public record ScenarioSavings(List<PersonSavings> perPerson, BigDecimal annualTotal, BigDecimal cumulativeFiveYears) {}

    public record TaxSavingsView(ScenarioSavings userDefined, ScenarioSavings modeled) {}

    public record ProjectionView(
            int startYear,
            int horizonYears,
            ProjectionEngine.ScenarioProjection userDefined,
            ProjectionEngine.ScenarioProjection modeled
    ) {}

    public record ExplanationView(List<ExplanationStep> steps, List<String> whatDiffers) {}
}
