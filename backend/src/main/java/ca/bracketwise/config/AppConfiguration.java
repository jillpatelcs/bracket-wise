package ca.bracketwise.config;

import ca.bracketwise.domain.calc.PlanningDefaults;
import ca.bracketwise.domain.calc.ScenarioCalculator;
import ca.bracketwise.domain.tax.TaxParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Bean
    public TaxParameters taxParameters(
            TaxConfigLoader loader,
            @Value("${bracketwise.tax-config:tax/tax-2026.json}") String path
    ) {
        return loader.loadTax(path);
    }

    @Bean
    public PlanningDefaults planningDefaults(TaxConfigLoader loader) {
        return loader.loadDefaults("tax/planning-defaults.json");
    }

    @Bean
    public ScenarioCalculator scenarioCalculator(TaxParameters taxParameters, PlanningDefaults planningDefaults) {
        return new ScenarioCalculator(taxParameters, planningDefaults);
    }
}
