package ca.bracketwise.config;

import ca.bracketwise.domain.calc.PlanningDefaults;
import ca.bracketwise.domain.tax.TaxParameters;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

@Component
public class TaxConfigLoader {
    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    public TaxParameters loadTax(String classpath) {
        TaxParameters params = read(classpath, TaxParameters.class);
        require(params.effectiveTaxYear() != 0, "effectiveTaxYear");
        requireText(params.version(), "version");
        requireText(params.source(), "source");
        requireText(params.retrievedOn(), "retrievedOn");
        require(params.federal() != null, "federal");
        requireText(params.federal().source(), "federal.source");
        requireText(params.federal().retrievedOn(), "federal.retrievedOn");
        require(params.ontario() != null, "ontario");
        requireText(params.ontario().source(), "ontario.source");
        requireText(params.ontario().retrievedOn(), "ontario.retrievedOn");
        require(params.federal().brackets() != null && !params.federal().brackets().isEmpty(), "federal.brackets");
        require(params.ontario().brackets() != null && !params.ontario().brackets().isEmpty(), "ontario.brackets");
        require(params.ontario().ohpBands() != null && !params.ontario().ohpBands().isEmpty(), "ontario.ohpBands");
        requirePositive(params.federal().lowestRate(), "federal.lowestRate");
        return params;
    }

    public PlanningDefaults loadDefaults(String classpath) {
        PlanningDefaults defaults = read(classpath, PlanningDefaults.class);
        requireText(defaults.version(), "version");
        requireText(defaults.source(), "source");
        requireText(defaults.retrievedOn(), "retrievedOn");
        requirePositive(defaults.allocationIncrement(), "allocationIncrement");
        return defaults;
    }

    private <T> T read(String classpath, Class<T> type) {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load configuration " + classpath + ": " + e.getMessage(), e);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required configuration field missing: " + field);
        }
    }

    private static void require(boolean ok, String field) {
        if (!ok) {
            throw new IllegalStateException("Required configuration field missing: " + field);
        }
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Required configuration field missing: " + field);
        }
    }
}
