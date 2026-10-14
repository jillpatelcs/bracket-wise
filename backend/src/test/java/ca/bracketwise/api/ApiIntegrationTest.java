package ca.bracketwise.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Test
    void health() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void corsAllowsConfiguredDevelopmentOrigin() throws Exception {
        mvc.perform(options("/api/v1/calculate")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Headers", containsString("content-type")));
    }

    @Test
    void corsDoesNotAllowUnconfiguredOrigin() throws Exception {
        mvc.perform(options("/api/v1/calculate")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void meta() throws Exception {
        mvc.perform(get("/api/v1/meta")).andExpect(status().isOk())
                .andExpect(jsonPath("$.supportedProvince").value("ON"))
                .andExpect(jsonPath("$.supportedTaxYear").value(2026));
    }

    @Test
    void calculateExampleCouple() throws Exception {
        mvc.perform(post("/api/v1/calculate").contentType(MediaType.APPLICATION_JSON).content(COUPLE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configVersion").value("synthetic-fixture-1"))
                .andExpect(jsonPath("$.estimatedTaxSavings.userDefined.annualTotal").value("1950.00"))
                .andExpect(jsonPath("$.modeledAllocation.label").value("Tax-aware modeled allocation under these assumptions"))
                .andExpect(jsonPath("$.explanation.steps[0].rule").value("R1_FHSA_FIRST"))
                .andExpect(jsonPath("$..per100").doesNotExist());
    }

    @Test
    void unknownProperty() throws Exception {
        mvc.perform(post("/api/v1/calculate").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"mode":"SINGLE","province":"ON","taxYear":2026,"monthlySavings":100,"extraField":true,
                         "assumptions":{"expectedAnnualReturn":0.04,"rrspWithdrawalTaxRate":0.2},
                         "persons":[{"id":"A","canadianTaxResidentForFullYear":true,"estimatedEmploymentIncomeFor2026":50000,
                         "availableTfsaRoom":1000,"availableRrspDeductionLimit":0,"fhsaEligible":false,"availableFhsaParticipationRoom":0}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PROPERTY"));
    }

    @Test
    void malformedJson() throws Exception {
        mvc.perform(post("/api/v1/calculate").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    void unsupportedMedia() throws Exception {
        mvc.perform(post("/api/v1/calculate").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void firstHomeWithoutFhsa() throws Exception {
        mvc.perform(post("/api/v1/calculate").contentType(MediaType.APPLICATION_JSON).content("""
                {"mode":"SINGLE","province":"ON","taxYear":2026,"monthlySavings":100,"fhsaFirstHomePlan":true,
                 "assumptions":{"expectedAnnualReturn":0.04,"rrspWithdrawalTaxRate":0.2},
                 "persons":[{"id":"A","canadianTaxResidentForFullYear":true,"estimatedEmploymentIncomeFor2026":50000,
                 "availableTfsaRoom":1000,"availableRrspDeductionLimit":0,"fhsaEligible":false,"availableFhsaParticipationRoom":0}]}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FIRST_HOME_PLAN_WITHOUT_ELIGIBLE_FHSA"));
    }

    @Test
    void partYearWarning() throws Exception {
        mvc.perform(post("/api/v1/calculate").contentType(MediaType.APPLICATION_JSON).content(COUPLE_JSON.replace(
                        "\"canadianTaxResidentForFullYear\": true,\n      \"estimatedEmploymentIncomeFor2026\": 45000",
                        "\"canadianTaxResidentForFullYear\": false,\n      \"estimatedEmploymentIncomeFor2026\": 45000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0].code").value("TAX_OPTIMIZATION_DISABLED_PART_YEAR"))
                .andExpect(jsonPath("$.perPersonTax").value(nullValue()));
    }

    @Test
    void deterministicRepeat() throws Exception {
        String a = mvc.perform(post("/api/v1/calculate").contentType(MediaType.APPLICATION_JSON).content(COUPLE_JSON))
                .andReturn().getResponse().getContentAsString();
        String b = mvc.perform(post("/api/v1/calculate").contentType(MediaType.APPLICATION_JSON).content(COUPLE_JSON))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(a, b);
    }

    private static final String COUPLE_JSON = """
            {
              "mode": "COUPLE",
              "province": "ON",
              "taxYear": 2026,
              "monthlySavings": 1500,
              "fhsaFirstHomePlan": true,
              "assumptions": { "expectedAnnualReturn": 0.05, "rrspWithdrawalTaxRate": 0.20 },
              "persons": [
                {
                  "id": "A", "label": "Person A", "isNewcomer": true, "arrivalDate": "2024-09-01",
                  "canadianTaxResidentForFullYear": true,
                  "estimatedEmploymentIncomeFor2026": 54000,
                  "availableTfsaRoom": 6000, "availableRrspDeductionLimit": 8000,
                  "fhsaEligible": true, "availableFhsaParticipationRoom": 3000
                },
                {
                  "id": "B", "label": "Person B", "isNewcomer": true, "arrivalDate": "2024-09-01",
                  "canadianTaxResidentForFullYear": true,
                  "estimatedEmploymentIncomeFor2026": 45000,
                  "availableTfsaRoom": 9000, "availableRrspDeductionLimit": 5000,
                  "fhsaEligible": true, "availableFhsaParticipationRoom": 4000
                }
              ],
              "userAllocation": [
                { "person": "A", "account": "TFSA", "percent": 20.00 },
                { "person": "A", "account": "RRSP", "percent": 20.00 },
                { "person": "A", "account": "FHSA", "percent": 10.00 },
                { "person": "B", "account": "TFSA", "percent": 30.00 },
                { "person": "B", "account": "RRSP", "percent": 10.00 },
                { "person": "B", "account": "FHSA", "percent": 10.00 }
              ]
            }
            """;
}
