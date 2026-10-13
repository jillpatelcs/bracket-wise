package ca.bracketwise.api;

import ca.bracketwise.api.dto.CalculateRequest;
import ca.bracketwise.domain.calc.HouseholdInput;
import ca.bracketwise.domain.calc.PlanningDefaults;
import ca.bracketwise.domain.calc.ScenarioCalculator;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CalculateController {
    private final ScenarioCalculator calculator;
    private final PlanningDefaults defaults;

    public CalculateController(ScenarioCalculator calculator, PlanningDefaults defaults) {
        this.calculator = calculator;
        this.defaults = defaults;
    }

    @PostMapping(value = "/calculate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> calculate(@Valid @RequestBody CalculateRequest request) {
        HouseholdInput input = RequestMapper.toDomain(request, defaults);
        return ResponseMapper.toJson(calculator.calculate(input));
    }
}
