package ca.bracketwise.api;

import ca.bracketwise.api.dto.CalculateRequest;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.allocation.PercentConverter;
import ca.bracketwise.domain.calc.HouseholdInput;
import ca.bracketwise.domain.calc.PlanningDefaults;

import java.math.BigDecimal;
import java.util.List;

public final class RequestMapper {
    private RequestMapper() {}

    public static HouseholdInput toDomain(CalculateRequest req, PlanningDefaults defaults) {
        String province = req.province() == null ? defaults.supportedProvince() : req.province();
        int year = req.taxYear() == null ? defaults.supportedTaxYear() : req.taxYear();
        boolean fhsaPlan = Boolean.TRUE.equals(req.fhsaFirstHomePlan());
        HouseholdInput.Assumptions assumptions;
        if (req.assumptions() == null) {
            assumptions = new HouseholdInput.Assumptions(
                    defaults.defaultExpectedAnnualReturn(),
                    defaults.defaultRrspWithdrawalTaxRate()
            );
        } else {
            assumptions = new HouseholdInput.Assumptions(
                    req.assumptions().expectedAnnualReturn() == null
                            ? defaults.defaultExpectedAnnualReturn()
                            : req.assumptions().expectedAnnualReturn(),
                    req.assumptions().rrspWithdrawalTaxRate() == null
                            ? defaults.defaultRrspWithdrawalTaxRate()
                            : req.assumptions().rrspWithdrawalTaxRate()
            );
        }
        List<HouseholdInput.PersonInput> persons = req.persons().stream().map(RequestMapper::person).toList();
        List<PercentConverter.PercentLine> alloc = req.userAllocation() == null ? null :
                req.userAllocation().stream()
                        .map(a -> new PercentConverter.PercentLine(a.person(), a.account(), a.percent()))
                        .toList();
        HouseholdInput.Mode mode = req.mode() == CalculateRequest.HouseholdMode.COUPLE
                ? HouseholdInput.Mode.COUPLE
                : HouseholdInput.Mode.SINGLE;
        return new HouseholdInput(mode, province, year, req.monthlySavings(), fhsaPlan, assumptions, persons, alloc);
    }

    private static HouseholdInput.PersonInput person(CalculateRequest.PersonDto p) {
        PersonId id = p.id() == null ? PersonId.A : p.id();
        boolean newcomer = Boolean.TRUE.equals(p.isNewcomer());
        boolean fullYear = Boolean.TRUE.equals(p.canadianTaxResidentForFullYear());
        boolean fhsa = Boolean.TRUE.equals(p.fhsaEligible());
        return new HouseholdInput.PersonInput(
                id,
                p.label(),
                newcomer,
                p.arrivalDate(),
                p.graduationDate(),
                p.employmentStartDate(),
                fullYear,
                p.estimatedEmploymentIncomeFor2026(),
                p.availableTfsaRoom(),
                p.availableRrspDeductionLimit(),
                fhsa,
                p.availableFhsaParticipationRoom()
        );
    }
}
