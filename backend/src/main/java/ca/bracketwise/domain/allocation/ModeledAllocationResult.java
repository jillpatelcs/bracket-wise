package ca.bracketwise.domain.allocation;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.PersonId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ModeledAllocationResult(
        Map<Destination, BigDecimal> amounts,
        Map<PersonId, Map<Account, BigDecimal>> unusedRoom,
        BigDecimal unallocated,
        String unallocatedReason,
        List<ExplanationStep> steps
) {}
