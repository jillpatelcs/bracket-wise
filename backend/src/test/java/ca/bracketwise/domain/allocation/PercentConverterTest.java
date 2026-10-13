package ca.bracketwise.domain.allocation;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.DomainException;
import ca.bracketwise.domain.Money;
import ca.bracketwise.domain.PersonId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PercentConverterTest {

    @Test
    void fractionalCentsAmpleRoom() {
        List<PercentConverter.ConvertedLine> out = PercentConverter.convert(
                10001,
                List.of(
                        line(PersonId.A, Account.TFSA, "33.33"),
                        line(PersonId.A, Account.RRSP, "33.33"),
                        line(PersonId.A, Account.FHSA, "33.34")
                ),
                ample()
        );
        assertEquals("33.33", Money.asString(out.get(0).amount()));
        assertEquals("33.33", Money.asString(out.get(1).amount()));
        assertEquals("33.35", Money.asString(out.get(2).amount()));
        assertEquals(10001, out.stream().mapToLong(l -> Money.toCents(l.amount())).sum());
    }

    @Test
    void tightRoomSkipsFullDestination() {
        Map<PersonId, Map<Account, Long>> rooms = ample();
        rooms.get(PersonId.A).put(Account.FHSA, 3334L);
        List<PercentConverter.ConvertedLine> out = PercentConverter.convert(
                10001,
                List.of(
                        line(PersonId.A, Account.TFSA, "33.33"),
                        line(PersonId.A, Account.RRSP, "33.33"),
                        line(PersonId.A, Account.FHSA, "33.34")
                ),
                rooms
        );
        assertEquals("33.34", Money.asString(out.get(0).amount()));
        assertEquals("33.33", Money.asString(out.get(1).amount()));
        assertEquals("33.34", Money.asString(out.get(2).amount()));
    }

    @Test
    void roundingCannotFitRejected() {
        Map<PersonId, Map<Account, Long>> rooms = new EnumMap<>(PersonId.class);
        Map<Account, Long> a = new EnumMap<>(Account.class);
        a.put(Account.TFSA, 50000L);
        a.put(Account.RRSP, 50000L);
        rooms.put(PersonId.A, a);
        DomainException ex = assertThrows(DomainException.class, () -> PercentConverter.convert(
                100001,
                List.of(
                        line(PersonId.A, Account.TFSA, "50.00"),
                        line(PersonId.A, Account.RRSP, "50.00")
                ),
                rooms
        ));
        assertEquals("ALLOCATION_ROUNDING_EXCEEDS_ROOM", ex.code());
    }

    @Test
    void floorOverRoomRejected() {
        Map<PersonId, Map<Account, Long>> rooms = ample();
        rooms.get(PersonId.A).put(Account.TFSA, 59999L);
        DomainException ex = assertThrows(DomainException.class, () -> PercentConverter.convert(
                100000,
                List.of(
                        line(PersonId.A, Account.TFSA, "60.00"),
                        line(PersonId.A, Account.RRSP, "40.00")
                ),
                rooms
        ));
        assertEquals("ALLOCATION_EXCEEDS_ROOM", ex.code());
    }

    @Test
    void zeroPercentNeverAdjusted() {
        List<PercentConverter.ConvertedLine> out = PercentConverter.convert(
                10000,
                List.of(
                        line(PersonId.A, Account.TFSA, "100.00"),
                        line(PersonId.A, Account.RRSP, "0.00")
                ),
                ample()
        );
        assertEquals(1, out.size());
        assertEquals("100.00", Money.asString(out.get(0).amount()));
    }

    @Test
    void inputOrderIndependent() {
        List<PercentConverter.PercentLine> lines = new ArrayList<>(List.of(
                line(PersonId.A, Account.FHSA, "33.34"),
                line(PersonId.A, Account.TFSA, "33.33"),
                line(PersonId.A, Account.RRSP, "33.33")
        ));
        List<PercentConverter.ConvertedLine> a = PercentConverter.convert(10001, lines, ample());
        Collections.reverse(lines);
        List<PercentConverter.ConvertedLine> b = PercentConverter.convert(10001, lines, ample());
        assertEquals(a, b);
    }

    @Test
    void largeBudgetTinyPercent() {
        List<PercentConverter.ConvertedLine> out = PercentConverter.convert(
                60_000_000,
                List.of(
                        line(PersonId.A, Account.TFSA, "0.01"),
                        line(PersonId.A, Account.RRSP, "99.99")
                ),
                ample()
        );
        assertEquals(60_000_000, out.stream().mapToLong(l -> Money.toCents(l.amount())).sum());
        long rawTfsa = 6000;
        assertTrue(Math.abs(Money.toCents(out.get(0).amount()) - rawTfsa) <= 1);
    }

    private static PercentConverter.PercentLine line(PersonId p, Account a, String pct) {
        return new PercentConverter.PercentLine(p, a, new BigDecimal(pct));
    }

    private static Map<PersonId, Map<Account, Long>> ample() {
        Map<PersonId, Map<Account, Long>> rooms = new EnumMap<>(PersonId.class);
        Map<Account, Long> a = new EnumMap<>(Account.class);
        a.put(Account.TFSA, 100_000_000L);
        a.put(Account.RRSP, 100_000_000L);
        a.put(Account.FHSA, 100_000_000L);
        rooms.put(PersonId.A, a);
        return rooms;
    }
}
