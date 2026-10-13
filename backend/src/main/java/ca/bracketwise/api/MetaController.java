package ca.bracketwise.api;

import ca.bracketwise.domain.calc.PlanningDefaults;
import ca.bracketwise.domain.tax.TaxParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MetaController {
    private final TaxParameters tax;
    private final PlanningDefaults defaults;

    public MetaController(TaxParameters tax, PlanningDefaults defaults) {
        this.tax = tax;
        this.defaults = defaults;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("supportedProvince", defaults.supportedProvince());
        m.put("supportedTaxYear", defaults.supportedTaxYear());
        m.put("configVersion", tax.version());
        m.put("configStatus", tax.status());
        m.put("configStatusNote", tax.statusNote());
        m.put("sources", List.of(
                Map.of("group", "federalBrackets", "url", tax.federal().source(), "retrievedOn", tax.federal().retrievedOn()),
                Map.of("group", "federalCredits", "url", tax.federal().creditsSource(), "retrievedOn", tax.federal().creditsRetrievedOn()),
                Map.of("group", "ontarioBrackets", "url", tax.ontario().source(), "retrievedOn", tax.ontario().retrievedOn()),
                Map.of("group", "ontarioSurtax", "url", tax.ontario().surtaxSource(), "retrievedOn", tax.ontario().retrievedOn()),
                Map.of("group", "ontarioHealthPremium", "url", tax.ontario().ohpSource(), "retrievedOn", tax.ontario().retrievedOn())
        ));
        m.put("limits", Map.of(
                "maxMonthlySavings", defaults.maxMonthlySavings().toPlainString(),
                "maxIncome", defaults.maxIncome().toPlainString(),
                "maxRequestBytes", defaults.maxRequestBytes()
        ));
        m.put("defaults", Map.of(
                "expectedAnnualReturn", defaults.defaultExpectedAnnualReturn().toPlainString(),
                "rrspWithdrawalTaxRate", defaults.defaultRrspWithdrawalTaxRate().toPlainString(),
                "allocationIncrement", defaults.allocationIncrement().toPlainString()
        ));
        return m;
    }
}
