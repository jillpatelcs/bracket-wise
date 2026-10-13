package ca.bracketwise.api;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.Money;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.allocation.ExplanationStep;
import ca.bracketwise.domain.calc.CalculationResult;
import ca.bracketwise.domain.projection.ProjectionEngine;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResponseMapper {
    private ResponseMapper() {}

    public static Map<String, Object> toJson(CalculationResult result) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("configVersion", result.configVersion());
        root.put("warnings", result.warnings().stream().map(w -> Map.of("code", w.code(), "message", w.message())).toList());
        root.put("assumptions", assumptions(result.assumptions()));
        root.put("perPersonTax", result.perPersonTax() == null ? null : result.perPersonTax().stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("person", t.person().name());
            m.put("components", Map.of(
                    "federal", Money.asString(t.components().federal()),
                    "ontario", Money.asString(t.components().ontario()),
                    "ontarioSurtax", Money.asString(t.components().ontarioSurtax()),
                    "ontarioHealthPremium", Money.asString(t.components().ontarioHealthPremium())
            ));
            m.put("simplifiedEstimatedIncomeTax", Money.asString(t.components().total()));
            m.put("averageEstimatedTaxRate", Money.asRate(t.averageEstimatedTaxRate()));
            return m;
        }).toList());
        root.put("availableRoom", result.availableRoom().stream().map(r -> Map.of(
                "person", r.person().name(),
                "tfsa", Money.asString(r.tfsa()),
                "rrspDeductionLimit", Money.asString(r.rrspDeductionLimit()),
                "fhsa", Money.asString(r.fhsa()),
                "fhsaEligible", r.fhsaEligible()
        )).toList());
        root.put("userAllocation", allocation(result.userAllocation()));
        root.put("modeledAllocation", allocation(result.modeledAllocation()));
        root.put("estimatedTaxSavings", taxSavings(result.estimatedTaxSavings()));
        root.put("projection", projection(result.projection()));
        root.put("explanation", explanation(result.explanation()));
        return root;
    }

    private static Map<String, Object> assumptions(CalculationResult.ResultAssumptions a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("expectedAnnualReturn", strip(a.expectedAnnualReturn()));
        m.put("rrspWithdrawalTaxRate", strip(a.rrspWithdrawalTaxRate()));
        m.put("horizonYears", a.horizonYears());
        m.put("contributionTiming", a.contributionTiming());
        m.put("fhsaFirstHomePlan", a.fhsaFirstHomePlan());
        m.put("fhsaTreatment", a.fhsaTreatment());
        m.put("allocationIncrement", Money.asString(a.allocationIncrement()));
        m.put("annualBudget", Money.asString(a.annualBudget()));
        m.put("allocationMethod", a.allocationMethod());
        return m;
    }

    private static Map<String, Object> allocation(CalculationResult.AllocationView view) {
        if (view == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        if (view.label() != null) {
            m.put("label", view.label());
        }
        m.put("allocations", view.allocations().stream().map(line -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("person", line.person().name());
            l.put("account", line.account().name());
            if (line.percent() != null) {
                l.put("percent", line.percent().setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString());
            }
            l.put("amount", Money.asString(line.amount()));
            return l;
        }).toList());
        m.put("unusedRoom", unused(view.unusedRoom()));
        m.put("unallocated", Money.asString(view.unallocated()));
        m.put("unallocatedReason", view.unallocatedReason());
        return m;
    }

    private static Map<String, Object> unused(Map<PersonId, Map<Account, BigDecimal>> unused) {
        Map<String, Object> outer = new LinkedHashMap<>();
        unused.forEach((person, rooms) -> {
            Map<String, String> inner = new LinkedHashMap<>();
            inner.put("tfsa", Money.asString(rooms.get(Account.TFSA)));
            inner.put("rrsp", Money.asString(rooms.get(Account.RRSP)));
            inner.put("fhsa", Money.asString(rooms.get(Account.FHSA)));
            outer.put(person.name(), inner);
        });
        return outer;
    }

    private static Map<String, Object> taxSavings(CalculationResult.TaxSavingsView view) {
        if (view == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userDefined", scenarioSavings(view.userDefined()));
        m.put("modeled", scenarioSavings(view.modeled()));
        return m;
    }

    private static Map<String, Object> scenarioSavings(CalculationResult.ScenarioSavings s) {
        if (s == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("perPerson", s.perPerson().stream().map(p -> Map.of(
                "person", p.person().name(),
                "totalDeductions", Money.asString(p.totalDeductions()),
                "taxAfterDeductions", Money.asString(p.taxAfterDeductions()),
                "estimatedTaxSavings", Money.asString(p.estimatedTaxSavings())
        )).toList());
        m.put("annualTotal", Money.asString(s.annualTotal()));
        m.put("cumulativeFiveYears", Money.asString(s.cumulativeFiveYears()));
        return m;
    }

    private static Map<String, Object> projection(CalculationResult.ProjectionView view) {
        if (view == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("startYear", view.startYear());
        m.put("horizonYears", view.horizonYears());
        m.put("userDefined", scenarioProjection(view.userDefined()));
        m.put("modeled", scenarioProjection(view.modeled()));
        return m;
    }

    private static Map<String, Object> scenarioProjection(ProjectionEngine.ScenarioProjection p) {
        if (p == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("householdPretaxByYear", p.householdPretaxByYear().stream().map(Money::asString).toList());
        m.put("yearFiveBalances", p.yearFiveBalances().stream().map(b -> Map.of(
                "person", b.person().name(),
                "account", b.account().name(),
                "pretax", Money.asString(b.pretax()),
                "estimatedAfterTax", Money.asString(b.estimatedAfterTax())
        )).toList());
        m.put("estimatedAfterTaxAccountValue", Money.asString(p.estimatedAfterTaxAccountValue()));
        m.put("taxSavingsIfSaved", Money.asString(p.taxSavingsIfSaved()));
        return m;
    }

    private static Map<String, Object> explanation(CalculationResult.ExplanationView view) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("steps", view.steps().stream().map(ResponseMapper::step).toList());
        m.put("whatDiffers", view.whatDiffers());
        return m;
    }

    private static Map<String, Object> step(ExplanationStep s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("step", s.step());
        m.put("person", s.person().name());
        m.put("account", s.account().name());
        m.put("rule", s.rule().name());
        m.put("amount", Money.asString(s.amount()));
        m.put("incrementCount", s.incrementCount());
        m.put("deductible", s.deductible());
        m.put("estimatedTaxSavings", Money.asString(s.estimatedTaxSavings()));
        m.put("effectiveSavingsRate", s.effectiveSavingsRate() == null ? null : Money.asRate(s.effectiveSavingsRate()));
        m.put("savingsRateMin", s.savingsRateMin() == null ? null : Money.asRate(s.savingsRateMin()));
        m.put("savingsRateMax", s.savingsRateMax() == null ? null : Money.asRate(s.savingsRateMax()));
        m.put("nextIncrement", s.nextIncrement() == null ? null : Map.of(
                "amount", Money.asString(s.nextIncrement().amount()),
                "estimatedTaxSavings", Money.asString(s.nextIncrement().estimatedTaxSavings()),
                "savingsRate", Money.asRate(s.nextIncrement().savingsRate())
        ));
        m.put("reason", s.reason());
        return m;
    }

    private static String strip(BigDecimal v) {
        return v.stripTrailingZeros().scale() <= 0 ? v.setScale(2).toPlainString() : v.toPlainString();
    }
}
