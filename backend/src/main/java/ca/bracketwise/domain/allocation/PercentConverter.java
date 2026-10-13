package ca.bracketwise.domain.allocation;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.DomainException;
import ca.bracketwise.domain.Money;
import ca.bracketwise.domain.PersonId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PercentConverter {

    public record PercentLine(PersonId person, Account account, BigDecimal percent) {}

    public record ConvertedLine(PersonId person, Account account, BigDecimal percent, BigDecimal amount) {}

    public static List<ConvertedLine> convert(
            long budgetCents,
            List<PercentLine> lines,
            Map<PersonId, Map<Account, Long>> roomCents
    ) {
        Map<String, PercentLine> unique = new LinkedHashMap<>();
        BigDecimal totalPercent = BigDecimal.ZERO;
        for (PercentLine line : lines) {
            String key = line.person().name() + ":" + line.account().name();
            unique.put(key, line);
            totalPercent = totalPercent.add(line.percent());
        }
        if (totalPercent.compareTo(new BigDecimal("100.00")) != 0) {
            throw new DomainException("ALLOCATION_TOTAL_NOT_100", "userAllocation",
                    "Allocation percentages must total exactly 100.00.");
        }

        List<DestinationWork> work = new ArrayList<>();
        for (Destination dest : Destination.canonical()) {
            PercentLine line = unique.get(dest.person().name() + ":" + dest.account().name());
            BigDecimal percent = line == null ? BigDecimal.ZERO : line.percent();
            if (percent.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Long room = roomCents.getOrDefault(dest.person(), Map.of()).get(dest.account());
            if (room == null) {
                throw new DomainException("ALLOCATION_EXCEEDS_ROOM", dest.field(),
                        "This destination is not eligible or has no room.");
            }
            BigDecimal raw = BigDecimal.valueOf(budgetCents)
                    .multiply(percent)
                    .divide(new BigDecimal("100"), 16, RoundingMode.DOWN);
            long base = raw.setScale(0, RoundingMode.FLOOR).longValueExact();
            BigDecimal fraction = raw.subtract(BigDecimal.valueOf(base));
            if (base > room) {
                throw new DomainException("ALLOCATION_EXCEEDS_ROOM", dest.field(),
                        "This allocation exceeds entered available room.");
            }
            work.add(new DestinationWork(dest, percent, room, raw, base, fraction, false));
        }

        long leftover = budgetCents - work.stream().mapToLong(w -> w.base).sum();
        List<DestinationWork> ordered = new ArrayList<>(work);
        ordered.sort(Comparator
                .comparing((DestinationWork w) -> w.fraction).reversed()
                .thenComparingInt(w -> w.dest.order()));

        while (leftover > 0) {
            boolean granted = false;
            for (DestinationWork w : ordered) {
                if (w.receivedAdjustment) {
                    continue;
                }
                if (w.base + 1 <= w.room) {
                    w.base += 1;
                    w.receivedAdjustment = true;
                    leftover -= 1;
                    granted = true;
                    break;
                }
            }
            if (!granted) {
                throw new DomainException("ALLOCATION_ROUNDING_EXCEEDS_ROOM", "userAllocation",
                        "This allocation cannot be placed within your entered room after cent rounding.");
            }
        }

        List<ConvertedLine> result = new ArrayList<>();
        Map<Destination, DestinationWork> byDest = new EnumMap<>(Destination.class);
        for (DestinationWork w : work) {
            byDest.put(w.dest, w);
        }
        for (Destination dest : Destination.canonical()) {
            DestinationWork w = byDest.get(dest);
            if (w != null) {
                result.add(new ConvertedLine(dest.person(), dest.account(), w.percent, Money.fromCents(w.base)));
            }
        }
        return result;
    }

    private static final class DestinationWork {
        private final Destination dest;
        private final BigDecimal percent;
        private final long room;
        private final BigDecimal raw;
        private long base;
        private final BigDecimal fraction;
        private boolean receivedAdjustment;

        private DestinationWork(Destination dest, BigDecimal percent, long room, BigDecimal raw, long base,
                                BigDecimal fraction, boolean receivedAdjustment) {
            this.dest = dest;
            this.percent = percent;
            this.room = room;
            this.raw = raw;
            this.base = base;
            this.fraction = fraction;
            this.receivedAdjustment = receivedAdjustment;
        }
    }
}
