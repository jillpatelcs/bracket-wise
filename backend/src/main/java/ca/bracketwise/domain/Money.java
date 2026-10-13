package ca.bracketwise.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    public static final RoundingMode HALF_UP = RoundingMode.HALF_UP;

    private Money() {}

    public static BigDecimal of(String value) {
        return new BigDecimal(value);
    }

    public static BigDecimal two(BigDecimal value) {
        return value.setScale(2, HALF_UP);
    }

    public static BigDecimal twoExact(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal rate4(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, 4, HALF_UP);
    }

    public static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
    }

    public static String asString(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String asRate(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    public static boolean hasMoreThanTwoDecimals(BigDecimal value) {
        return value.stripTrailingZeros().scale() > 2;
    }

    public static boolean hasMoreThanNDecimals(BigDecimal value, int n) {
        return value.stripTrailingZeros().scale() > n;
    }
}
