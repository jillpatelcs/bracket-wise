package ca.bracketwise.domain.allocation;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.Money;
import ca.bracketwise.domain.PersonId;
import ca.bracketwise.domain.tax.TaxCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RuleBasedAllocator {
    private final TaxCalculator tax;
    private final long incrementCents;

    public RuleBasedAllocator(TaxCalculator tax, BigDecimal increment) {
        this.tax = tax;
        this.incrementCents = Money.toCents(increment);
    }

    public ModeledAllocationResult allocate(
            long budgetCents,
            Map<PersonId, PersonState> people,
            boolean fhsaFirstHomePlan,
            BigDecimal withdrawalRate
    ) {
        Map<Destination, Long> remainingRoom = new EnumMap<>(Destination.class);
        Map<PersonId, BigDecimal> deduction = new EnumMap<>(PersonId.class);
        Map<Destination, Long> allocated = new EnumMap<>(Destination.class);
        for (PersonState p : people.values()) {
            deduction.put(p.id(), BigDecimal.ZERO);
            putRoom(remainingRoom, allocated, p);
        }

        long budgetLeft = budgetCents;
        List<RawIncrement> raw = new ArrayList<>();

        while (budgetLeft > 0) {
            Choice choice = choose(people, remainingRoom, deduction, budgetLeft, fhsaFirstHomePlan, withdrawalRate);
            if (choice == null) {
                break;
            }
            remainingRoom.put(choice.destination, remainingRoom.get(choice.destination) - choice.deltaCents);
            allocated.merge(choice.destination, choice.deltaCents, Long::sum);
            budgetLeft -= choice.deltaCents;
            if (choice.deductible) {
                deduction.merge(choice.destination.person(), choice.amount(), BigDecimal::add);
            }
            raw.add(new RawIncrement(choice.destination, choice.rule, choice.amount(), choice.savings, choice.rate, choice.deductible));
        }

        List<ExplanationStep> steps = merge(raw, remainingRoom, deduction, people, budgetLeft, fhsaFirstHomePlan, withdrawalRate);

        Map<Destination, BigDecimal> amounts = new EnumMap<>(Destination.class);
        allocated.forEach((d, c) -> {
            if (c > 0) {
                amounts.put(d, Money.fromCents(c));
            }
        });

        Map<PersonId, Map<Account, BigDecimal>> unused = new EnumMap<>(PersonId.class);
        for (PersonState p : people.values()) {
            Map<Account, BigDecimal> rooms = new EnumMap<>(Account.class);
            rooms.put(Account.TFSA, Money.fromCents(remainingRoom.getOrDefault(Destination.of(p.id(), Account.TFSA), 0L)));
            rooms.put(Account.RRSP, Money.fromCents(remainingRoom.getOrDefault(Destination.of(p.id(), Account.RRSP), 0L)));
            rooms.put(Account.FHSA, p.fhsaEligible()
                    ? Money.fromCents(remainingRoom.getOrDefault(Destination.of(p.id(), Account.FHSA), 0L))
                    : Money.ZERO);
            unused.put(p.id(), rooms);
        }

        BigDecimal unallocated = Money.fromCents(budgetLeft);
        String reason = budgetLeft > 0 ? "ROOM_EXHAUSTED" : null;
        return new ModeledAllocationResult(amounts, unused, unallocated, reason, steps);
    }

    private void putRoom(Map<Destination, Long> remainingRoom, Map<Destination, Long> allocated, PersonState p) {
        remainingRoom.put(Destination.of(p.id(), Account.TFSA), Money.toCents(p.tfsaRoom()));
        remainingRoom.put(Destination.of(p.id(), Account.RRSP), Money.toCents(p.rrspRoom()));
        if (p.fhsaEligible()) {
            remainingRoom.put(Destination.of(p.id(), Account.FHSA), Money.toCents(p.fhsaRoom()));
        }
        allocated.put(Destination.of(p.id(), Account.TFSA), 0L);
        allocated.put(Destination.of(p.id(), Account.RRSP), 0L);
        if (p.fhsaEligible()) {
            allocated.put(Destination.of(p.id(), Account.FHSA), 0L);
        }
    }

    private Choice choose(
            Map<PersonId, PersonState> people,
            Map<Destination, Long> remainingRoom,
            Map<PersonId, BigDecimal> deduction,
            long budgetLeft,
            boolean fhsaFirstHomePlan,
            BigDecimal withdrawalRate
    ) {
        List<Candidate> candidates = candidates(people, remainingRoom, deduction, budgetLeft);
        if (candidates.isEmpty()) {
            return null;
        }
        if (fhsaFirstHomePlan) {
            Choice r1 = pick(filter(candidates, c -> c.destination.account() == Account.FHSA),
                    AllocationRule.R1_FHSA_FIRST, true, false);
            if (r1 != null) {
                return r1;
            }
        }
        Choice r2 = pick(filter(candidates, c -> isDeductibleLikeRrsp(c, fhsaFirstHomePlan) && greaterThan(c.rate, withdrawalRate)),
                AllocationRule.R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE, true, true);
        if (r2 != null) {
            return r2;
        }
        Choice r3 = pick(filter(candidates, c -> c.destination.account() == Account.TFSA),
                AllocationRule.R3_TFSA, false, false);
        if (r3 != null) {
            return r3;
        }
        return pick(filter(candidates, c -> isDeductibleLikeRrsp(c, fhsaFirstHomePlan)),
                AllocationRule.R4_REMAINING_BUDGET, true, true);
    }

    private boolean isDeductibleLikeRrsp(Candidate c, boolean fhsaFirstHomePlan) {
        if (c.destination.account() == Account.RRSP) {
            return true;
        }
        return c.destination.account() == Account.FHSA && !fhsaFirstHomePlan;
    }

    private List<Candidate> filter(List<Candidate> candidates, java.util.function.Predicate<Candidate> predicate) {
        return candidates.stream().filter(predicate).toList();
    }

    private Choice pick(List<Candidate> list, AllocationRule rule, boolean byRate, boolean fhsaBeforeRrsp) {
        if (list.isEmpty()) {
            return null;
        }
        Candidate best = list.get(0);
        for (Candidate c : list) {
            if (better(c, best, byRate, fhsaBeforeRrsp)) {
                best = c;
            }
        }
        return new Choice(best.destination, rule, best.deltaCents, best.savings, best.rate, best.deductible);
    }

    private boolean better(Candidate a, Candidate best, boolean byRate, boolean fhsaBeforeRrsp) {
        if (byRate) {
            int cmp = a.rate.compareTo(best.rate);
            if (cmp > 0) {
                return true;
            }
            if (cmp < 0) {
                return false;
            }
        }
        if (fhsaBeforeRrsp) {
            int acc = accountRank(a.destination.account()) - accountRank(best.destination.account());
            if (acc != 0) {
                return acc < 0;
            }
        }
        return a.destination.person().ordinal() < best.destination.person().ordinal();
    }

    private int accountRank(Account account) {
        if (account == Account.FHSA) {
            return 0;
        }
        if (account == Account.RRSP) {
            return 1;
        }
        return 2;
    }

    private boolean greaterThan(BigDecimal rate, BigDecimal w) {
        return rate.compareTo(w) > 0;
    }

    private List<Candidate> candidates(
            Map<PersonId, PersonState> people,
            Map<Destination, Long> remainingRoom,
            Map<PersonId, BigDecimal> deduction,
            long budgetLeft
    ) {
        List<Candidate> list = new ArrayList<>();
        for (Destination dest : Destination.canonical()) {
            Long room = remainingRoom.get(dest);
            if (room == null || room <= 0) {
                continue;
            }
            PersonState person = people.get(dest.person());
            if (person == null) {
                continue;
            }
            long delta = Math.min(incrementCents, Math.min(budgetLeft, room));
            if (delta <= 0) {
                continue;
            }
            BigDecimal amount = Money.fromCents(delta);
            boolean deductible = dest.account() != Account.TFSA;
            BigDecimal savings = deductible
                    ? tax.estimatedTaxSavings(person.income(), deduction.get(person.id()), amount)
                    : Money.ZERO;
            BigDecimal rate = deductible
                    ? savings.divide(amount, 16, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            list.add(new Candidate(dest, delta, savings, rate, deductible));
        }
        return list;
    }

    private List<ExplanationStep> merge(
            List<RawIncrement> raw,
            Map<Destination, Long> remainingRoom,
            Map<PersonId, BigDecimal> deduction,
            Map<PersonId, PersonState> people,
            long budgetLeft,
            boolean fhsaFirstHomePlan,
            BigDecimal withdrawalRate
    ) {
        List<ExplanationStep> steps = new ArrayList<>();
        int i = 0;
        int stepNo = 1;
        while (i < raw.size()) {
            RawIncrement first = raw.get(i);
            int j = i;
            BigDecimal amount = BigDecimal.ZERO;
            BigDecimal savings = BigDecimal.ZERO;
            BigDecimal min = first.rate;
            BigDecimal max = first.rate;
            int count = 0;
            while (j < raw.size()
                    && raw.get(j).destination == first.destination
                    && raw.get(j).rule == first.rule) {
                amount = amount.add(raw.get(j).amount);
                savings = savings.add(raw.get(j).savings);
                if (first.deductible) {
                    min = min.min(raw.get(j).rate);
                    max = max.max(raw.get(j).rate);
                }
                count++;
                j++;
            }
            ExplanationStep.NextIncrement next = null;
            if ((first.rule == AllocationRule.R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE
                    || first.rule == AllocationRule.R4_REMAINING_BUDGET)
                    && j < raw.size()
                    && remainingRoom.getOrDefault(first.destination, 0L) > 0) {
                PersonState person = people.get(first.destination.person());
                long peek = Math.min(incrementCents, remainingRoom.get(first.destination));
                BigDecimal peekAmt = Money.fromCents(peek);
                BigDecimal d = deduction.get(person.id());
                // deduction already includes all applied increments; peek uses current D
                // but that includes later steps too. Recompute deduction after this merged step only.
            }
            BigDecimal deductionAfterThis = deductionAfter(raw, j, first.destination.person());
            if ((first.rule == AllocationRule.R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE
                    || first.rule == AllocationRule.R4_REMAINING_BUDGET)
                    && roomAfter(raw, j, first.destination, people) > 0
                    && budgetAfter(raw, j, budgetLeft + sumRemainingIncrements(raw, j)) > 0) {
                PersonState person = people.get(first.destination.person());
                long roomLeft = roomAfter(raw, j, first.destination, people);
                long budgetAfterStep = sumRemainingIncrements(raw, j) + budgetLeft;
                long peek = Math.min(incrementCents, Math.min(roomLeft, budgetAfterStep));
                if (peek > 0) {
                    BigDecimal peekAmt = Money.fromCents(peek);
                    BigDecimal peekSav = tax.estimatedTaxSavings(person.income(), deductionAfterThis, peekAmt);
                    BigDecimal peekRate = peekSav.divide(peekAmt, 16, RoundingMode.HALF_UP);
                    boolean ruleStill = first.rule == AllocationRule.R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE
                            ? peekRate.compareTo(withdrawalRate) > 0
                            : peekRate.compareTo(withdrawalRate) <= 0;
                    if (!ruleStill) {
                        next = new ExplanationStep.NextIncrement(
                                peekAmt,
                                Money.two(peekSav),
                                peekRate.setScale(4, RoundingMode.HALF_UP)
                        );
                    }
                }
            }
            BigDecimal effective = first.deductible ? Money.rate4(savings, amount) : null;
            BigDecimal min4 = first.deductible ? min.setScale(4, RoundingMode.HALF_UP) : null;
            BigDecimal max4 = first.deductible ? max.setScale(4, RoundingMode.HALF_UP) : null;
            steps.add(new ExplanationStep(
                    stepNo++,
                    first.destination.person(),
                    first.destination.account(),
                    first.rule,
                    Money.twoExact(amount),
                    count,
                    first.deductible,
                    Money.two(savings),
                    effective,
                    min4,
                    max4,
                    next,
                    reason(first, amount, count, min4, max4, next, withdrawalRate, fhsaFirstHomePlan, steps.size())
            ));
            i = j;
        }
        return steps;
    }

    private BigDecimal deductionAfter(List<RawIncrement> raw, int exclusiveEnd, PersonId person) {
        BigDecimal d = BigDecimal.ZERO;
        for (int k = 0; k < exclusiveEnd; k++) {
            if (raw.get(k).destination.person() == person && raw.get(k).deductible) {
                d = d.add(raw.get(k).amount);
            }
        }
        return d;
    }

    private long roomAfter(List<RawIncrement> raw, int exclusiveEnd, Destination dest, Map<PersonId, PersonState> people) {
        PersonState p = people.get(dest.person());
        long room = switch (dest.account()) {
            case TFSA -> Money.toCents(p.tfsaRoom());
            case RRSP -> Money.toCents(p.rrspRoom());
            case FHSA -> Money.toCents(p.fhsaRoom());
        };
        for (int k = 0; k < exclusiveEnd; k++) {
            if (raw.get(k).destination == dest) {
                room -= Money.toCents(raw.get(k).amount);
            }
        }
        return room;
    }

    private long sumRemainingIncrements(List<RawIncrement> raw, int from) {
        long sum = 0;
        for (int k = from; k < raw.size(); k++) {
            sum += Money.toCents(raw.get(k).amount);
        }
        return sum;
    }

    private long budgetAfter(List<RawIncrement> raw, int from, long leftoverPlusRest) {
        return leftoverPlusRest;
    }

    private String reason(
            RawIncrement first,
            BigDecimal amount,
            int count,
            BigDecimal min4,
            BigDecimal max4,
            ExplanationStep.NextIncrement next,
            BigDecimal w,
            boolean fhsaFirstHomePlan,
            int priorSteps
    ) {
        String wPct = w.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).toPlainString();
        return switch (first.rule) {
            case R1_FHSA_FIRST -> priorSteps == 0
                    ? "FHSA is used first: contributions are deductible and your first-home planning assumption treats qualifying withdrawals as tax-free."
                    : "FHSA is used first under the first-home planning assumption, continuing with the next eligible person after room on the previous FHSA destination was used.";
            case R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE -> r2Reason(first, amount, count, min4, max4, next, wPct);
            case R3_TFSA -> "No remaining deductible destination exceeds the assumed withdrawal-tax rate, so TFSA is used.";
            case R4_REMAINING_BUDGET ->
                    "Remaining budget is placed in deductible accounts whose increment savings rate does not exceed your assumed "
                            + wPct + "% RRSP withdrawal-tax rate.";
        };
    }

    private String r2Reason(
            RawIncrement first,
            BigDecimal amount,
            int count,
            BigDecimal min4,
            BigDecimal max4,
            ExplanationStep.NextIncrement next,
            String wPct
    ) {
        String account = first.destination.account().name();
        StringBuilder sb = new StringBuilder();
        if (min4 != null && max4 != null && min4.compareTo(max4) != 0) {
            sb.append(account).append(" room was filled in ").append(count)
                    .append(" increments totalling $").append(Money.asString(amount))
                    .append(". Estimated tax savings rates ranged from ")
                    .append(pct(min4)).append("% to ").append(pct(max4))
                    .append("%, all above your assumed ").append(wPct).append("% withdrawal-tax rate.");
        } else {
            sb.append(account)
                    .append(" is used while each increment's estimated tax savings rate exceeds your assumed ")
                    .append(wPct).append("% RRSP withdrawal-tax rate.");
        }
        if (next != null) {
            sb.append(" The next increment ($").append(Money.asString(next.amount()))
                    .append(") would have saved an estimated $").append(Money.asString(next.estimatedTaxSavings()))
                    .append(" (").append(pct(next.savingsRate())).append("%), which does not exceed ")
                    .append(wPct).append("%.");
        }
        return sb.toString();
    }

    private static String pct(BigDecimal rate) {
        return rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public record PersonState(
            PersonId id,
            BigDecimal income,
            BigDecimal tfsaRoom,
            BigDecimal rrspRoom,
            boolean fhsaEligible,
            BigDecimal fhsaRoom
    ) {}

    private record Candidate(Destination destination, long deltaCents, BigDecimal savings, BigDecimal rate, boolean deductible) {}

    private record Choice(Destination destination, AllocationRule rule, long deltaCents, BigDecimal savings, BigDecimal rate, boolean deductible) {
        BigDecimal amount() {
            return Money.fromCents(deltaCents);
        }
    }

    private record RawIncrement(Destination destination, AllocationRule rule, BigDecimal amount, BigDecimal savings, BigDecimal rate, boolean deductible) {}
}
