package org.checkerframework.common.value;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Range Class models an 64-bit 2 complement integral interval, such as all integers between 1
 * and 10, inclusive.
 *
 * @author JasonMrX
 */
public class Range {

    /** The value 'from'. */
    public final long from;

    /** The value 'to'. */
    public final long to;

    /** A range containing all possible values. */
    public static final Range EVERYTHING = new Range(Long.MIN_VALUE, Long.MAX_VALUE);

    public static final Range INT_EVERYTHING = new Range(Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final Range SHORT_EVERYTHING = new Range(Short.MIN_VALUE, Short.MAX_VALUE);

    /** The empty range. */
    public static final Range NOTHING = new Range(true);

    /**
     * Constructs a range with its bounds specified by two parameters, "from" and "to".
     *
     * @param from the lower bound (inclusive)
     * @param to the higher bound (inclusive)
     */
    public Range(long from, long to) {
        if (!(from <= to)) {
            throw new IllegalArgumentException(String.format("Invalid Range: %s %s", from, to));
        }
        this.from = from;
        this.to = to;
    }

    /** Creates the singleton empty range */
    private Range(boolean isEmpty) {
        if (!isEmpty) {
            throw new IllegalArgumentException();
        }
        this.from = Long.MAX_VALUE;
        this.to = Long.MIN_VALUE;
    }

    @Override
    public String toString() {
        return String.format("[%s..%s]", from, to);
    }

    /** Return true if this range contains everything. */
    public boolean isEverything() {
        return this == EVERYTHING;
    }

    /** Return true if this range contains nothing. */
    public boolean isNothing() {
        return this == NOTHING;
    }

    /**
     * Converts the elements in this range to int type as if they are overflow in the case of
     * 32-bits int
     */
    public Range intRange() {
        if (this.isWiderThan(Integer.MAX_VALUE - Integer.MIN_VALUE + 1)) {
            return INT_EVERYTHING;
        } else {
            int intFrom = (int) this.from;
            int intTo = (int) this.to;
            if (intFrom <= intTo) {
                return new Range(intFrom, intTo);
            } else {
                return INT_EVERYTHING;
            }
        }
    }

    /**
     * Converts the elements in this range to short type as if they are overflow in the case of
     * 16-bits short int
     */
    public Range shortRange() {
        if (this.isWiderThan(Short.MAX_VALUE - Short.MIN_VALUE + 1)) {
            return SHORT_EVERYTHING;
        } else {
            short shortFrom = (short) this.from;
            short shortTo = (short) this.to;
            if (shortFrom <= shortTo) {
                return new Range(shortFrom, shortTo);
            } else {
                return SHORT_EVERYTHING;
            }
        }
    }

    /** Returns true if the element is contained in this range. */
    public boolean contains(long element) {
        return from <= element && element <= to;
    }

    /**
     * Returns the smallest range that includes all values contained in either of the two ranges. We
     * call this the union of two ranges.
     *
     * @param right a range to union with this range
     * @return a range resulting from the union of the specified range and this range
     */
    public Range union(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        long resultFrom = Math.min(from, right.from);
        long resultTo = Math.max(to, right.to);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns the smallest range that includes all values contained in both of the two ranges. We
     * call this the intersection of two ranges. If there is no overlap between the two ranges, an
     * empty range would be returned.
     *
     * @param right the range to intersect with this range
     * @return a range resulting from the intersection of the specified range and this range
     */
    public Range intersect(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        long resultFrom = Math.max(from, right.from);
        long resultTo = Math.min(to, right.to);
        if (resultFrom <= resultTo) {
            return new Range(resultFrom, resultTo);
        } else {
            return NOTHING;
        }
    }

    /**
     * Returns the smallest range that includes all possible values resulting from adding an
     * arbitrary value in the specified range to an arbitrary value in this range. We call this We
     * call this the addition of two ranges.
     *
     * @param right a range to be added to this range
     * @return the range resulting from the addition of the specified range and this range
     */
    public Range plus(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (this.isWithinInteger() && right.isWithinInteger()) {
            long resultFrom = from + right.from;
            long resultTo = to + right.to;
            return new Range(resultFrom, resultTo);
        } else {
            BigInteger bigFrom = BigInteger.valueOf(from).add(BigInteger.valueOf(right.from));
            BigInteger bigTo = BigInteger.valueOf(to).add(BigInteger.valueOf(right.to));
            return bigRange2LongRange(bigFrom, bigTo);
        }
    }

    /**
     * Returns the smallest range that includes all possible values resulting from subtracting an
     * arbitrary value in the specified range from an arbitrary value in this range. We call this
     * the subtraction of two ranges.
     *
     * @param right the range to be subtracted from this range
     * @return the range resulting from subtracting the specified range from this range
     */
    public Range minus(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (this.isWithinInteger() && right.isWithinInteger()) {
            long resultFrom = from - right.to;
            long resultTo = to - right.from;
            return new Range(resultFrom, resultTo);
        } else {
            BigInteger bigFrom = BigInteger.valueOf(from).subtract(BigInteger.valueOf(right.to));
            BigInteger bigTo = BigInteger.valueOf(to).subtract(BigInteger.valueOf(right.from));
            return bigRange2LongRange(bigFrom, bigTo);
        }
    }

    /**
     * Returns the smallest range that includes all possible values resulting from multiplying an
     * arbitrary value in the specified range by an arbitrary value in this range. We call this the
     * multiplication of two ranges.
     *
     * @param right the specified range to be multiplied by this range
     * @return the range resulting from multiplying the specified range by this range
     */
    public Range times(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (this.isWithinInteger() && right.isWithinInteger()) {
            List<Long> possibleValues =
                    Arrays.asList(
                            from * right.from, from * right.to, to * right.from, to * right.to);
            return new Range(Collections.min(possibleValues), Collections.max(possibleValues));
        } else {
            List<BigInteger> bigPossibleValues =
                    Arrays.asList(
                            BigInteger.valueOf(from).multiply(BigInteger.valueOf(right.from)),
                            BigInteger.valueOf(from).multiply(BigInteger.valueOf(right.to)),
                            BigInteger.valueOf(to).multiply(BigInteger.valueOf(right.from)),
                            BigInteger.valueOf(to).multiply(BigInteger.valueOf(right.to)));
            BigInteger bigFrom = Collections.min(bigPossibleValues);
            BigInteger bigTo = Collections.max(bigPossibleValues);
            return bigRange2LongRange(bigFrom, bigTo);
        }
    }

    /**
     * Returns the smallest range that includes all possible values resulting from dividing (integer
     * division) an arbitrary value in this range by an arbitrary value in the specified range. We
     * call this the division of two ranges.
     *
     * @param right the specified range by which this range is divided
     * @return the range resulting from dividing this range by the specified range
     */
    public Range divide(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (right.contains(0)) {
            // Should warn division by zero
            return EVERYTHING;
        }

        if (from == Long.MIN_VALUE && right.contains(-1)) {
            // Special cases that involve overflow.
            // Here the values in the right range should all be negative.
            if (from != to) {
                // Special case 1:
                // This range should contain Long.MIN_VALUE and Long.MIN_VALUE + 1, which make the
                // result range EVERYTHING because the right range contains -1.
                return EVERYTHING;
            } else if (right.from != right.to) {
                // Special case 2:
                // This range should contains Long.MIN_VALUE only, and the right range should
                // contains at least -1 and -2. It is obvious that the result range in this case
                // is from Long.MIN_VALUE to Long.MIN_VALUE / -2.
                return new Range(Long.MIN_VALUE, Long.MIN_VALUE / -2);
            } else {
                // Special case 3:
                // Both this range and the right range contain only one value, Long.MIN_VALUE and -1
                // respectively. It means that the result range contains a single value
                // Long.MIN_VALUE / -1 = Long.MIN_VALUE
                return new Range(Long.MIN_VALUE, Long.MIN_VALUE);
            }
        }

        long resultFrom;
        long resultTo;
        // We shouldn't worry about the overflow issue starting from here.
        // To facilitate the calculation of the result range, we categorize all the scenarios into 9
        // different cases:
        // 1. this: po, right: nn
        // 2. this: po, right: np
        // 3. this: po, right: us
        // 4. this: ne, right: nn
        // 5. this: ne, right: np
        // 6. this: ne, right: us
        // 7. this: us, right: nn
        // 8. this: us, right: np
        // 9. this: us, right: us
        // (note: po=>position, ne=>negative, us:=>unknown sign, np=>non-position, nn=>non-negative)
        // The categorization corresponds to the following control flow branches.
        // It's not difficult to verify the calculation of each case.
        if (from > 0 && right.from >= 0) {
            resultFrom = from / Math.max(right.to, 1);
            resultTo = to / Math.max(right.from, 1);
        } else if (from > 0 && right.to <= 0) {
            resultFrom = to / Math.min(right.to, -1);
            resultTo = from / Math.min(right.from, -1);
        } else if (from > 0) {
            resultFrom = -to;
            resultTo = to;
        } else if (to < 0 && right.from >= 0) {
            resultFrom = from / Math.max(right.from, 1);
            resultTo = to / Math.max(right.to, 1);
        } else if (to < 0 && right.to <= 0) {
            resultFrom = to / Math.min(right.from, -1);
            resultTo = from / Math.min(right.to, -1);
        } else if (to < 0) {
            resultFrom = from;
            resultTo = -from;
        } else if (right.from >= 0) {
            resultFrom = from / Math.max(right.from, 1);
            resultTo = to / Math.max(right.from, 1);
        } else if (right.to <= 0) {
            resultFrom = to / Math.min(right.to, -1);
            resultTo = from / Math.min(right.to, -1);
        } else {
            resultFrom = Math.min(from, -to);
            resultTo = Math.max(-from, to);
        }
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns a range that includes all possible values of the remainder of dividing an arbitrary
     * value in this range by an arbitrary value in the specified range.
     *
     * @param right the specified range by which this range is divided
     * @return the range of the remainder of dividing this range by the specified range. Note that
     *     this range might not be the smallest range that includes all the possible values.
     */
    public Range remainder(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (right.contains(0)) {
            // should warn division by zero
            return EVERYTHING;
        }

        List<Long> possibleValues =
                Arrays.asList(
                        0L,
                        Math.min(from, Math.abs(right.from) - 1),
                        Math.min(from, Math.abs(right.to) - 1),
                        Math.min(to, Math.abs(right.from) - 1),
                        Math.min(to, Math.abs(right.to) - 1),
                        Math.max(from, -Math.abs(right.from) + 1),
                        Math.max(from, -Math.abs(right.to) + 1),
                        Math.max(to, -Math.abs(right.from) + 1),
                        Math.max(to, -Math.abs(right.to) + 1));
        return new Range(Collections.min(possibleValues), Collections.max(possibleValues));
    }

    /**
     * Returns a range that includes all possible values resulting from left shifting an arbitrary
     * value in this range by an arbitrary number of bits in the specified range. We call this the
     * left shift operation of a range.
     *
     * @param right the range of bits by which this range is left shifted
     * @return the range resulting from left shifting this range by the specified range
     */
    public Range shiftLeft(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (this.isWithinInteger() && right.from >= 0 && right.to <= 31) {
            // The long type can handle the cases with shift distance no longer than 31
            long resultFrom = from << (from >= 0 ? right.from : right.to);
            long resultTo = to << (to >= 0 ? right.to : right.from);
            return new Range(resultFrom, resultTo);
        } else if (right.from >= 0 && right.to <= 63) {
            // if the shift distance is within 0 to 63, the calculation can be handled by BigInteger
            BigInteger bigFrom =
                    BigInteger.valueOf(from)
                            .shiftLeft(from >= 0 ? (int) right.from : (int) right.to);
            BigInteger bigTo =
                    BigInteger.valueOf(to).shiftLeft(to >= 0 ? (int) right.to : (int) right.from);
            return bigRange2LongRange(bigFrom, bigTo);
        } else {
            // In other cases, we give up the calculation and return EVERYTHING (rare in practice)
            return EVERYTHING;
        }
    }

    /**
     * Returns a range that includes all possible values resulting from signed right shifting an
     * arbitrary value in this range by an arbitrary number of bits in the specified range. We call
     * this the signed right shift operation of a range.
     *
     * @param right the range of bits by which this range is signed right shifted
     * @return the range resulting from signed right shifting this range by the specified range
     */
    public Range signedShiftRight(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (this.isWithinInteger() && right.from >= 0 && right.to <= 31) {
            // The long type can handle the cases with shift distance no longer than 31
            long resultFrom = from >> (from >= 0 ? right.to : right.from);
            long resultTo = to >> (to >= 0 ? right.from : right.to);
            return new Range(resultFrom, resultTo);
        } else {
            // Signed shift right operation for long type cannot be simulated with BigInteger
            // Give up the calculation and return EVERYTHING instead.
            return EVERYTHING;
        }
    }

    /**
     * Returns the range of a variable that falls within this range after applying the unary plus
     * operation (which is a no-op).
     *
     * @return this range
     */
    public Range unaryPlus() {
        return this;
    }

    /**
     * Returns the range of a variable that falls within this range after applying unary minus
     * operation.
     *
     * @return the resulted range of applying unary minus on an arbitrary value in this range
     */
    public Range unaryMinus() {
        if (this.isNothing()) {
            return NOTHING;
        }

        if (from == Long.MIN_VALUE && from != to) {
            // the only case that needs special handling because of overflow
            return EVERYTHING;
        }

        return new Range(-to, -from);
    }

    /**
     * Returns the range of a variable that falls within this range after applying bitwise
     * complement operation.
     *
     * @return the resulted range of applying bitwise complement on an arbitrary value in this range
     */
    public Range bitwiseComplement() {
        if (this.isNothing()) {
            return NOTHING;
        }

        return new Range(~to, ~from);
    }

    /**
     * Determines a refined range of a variable that fall within this {@code Range} under the
     * condition that this variable is less than another variable that fall within the specified
     * {@code Range}. This is used for calculating the control flow refined result of the &lt;
     * operator. i.e.
     *
     * <pre>
     * <code>
     *     {@literal @}IntRange(from = 0, to = 10) int a;
     *     {@literal @}IntRange(from = 3, to = 5) int b;
     *     ...
     *     if (a &lt; b) {
     *         // range of <i>a</i> is now refined to [0, 4] because a value in range [5, 10]
     *         // cannot be smaller than variable <i>b</i> with range [3, 5]
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with.
     * @return the refined {@code Range}.
     */
    public Range refineLessThan(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (right.to == Long.MIN_VALUE) {
            return NOTHING;
        }

        long resultTo = Math.min(to, right.to - 1);
        if (from <= resultTo) {
            return new Range(from, resultTo);
        } else {
            return NOTHING;
        }
    }

    /**
     * Determines a refined range of a variable that fall within this {@code Range} under the
     * condition that this variable is less than or equal to another variable that fall within the
     * specified {@code Range}. This is used for calculating the control flow refined result of the
     * &lt;= operator. i.e.
     *
     * <pre>
     * <code>
     *     {@literal @}IntRange(from = 0, to = 10) int a;
     *     {@literal @}IntRange(from = 3, to = 5) int b;
     *     ...
     *     if (a &lt;= b) {
     *         // range of <i>a</i> is now refined to [0, 5] because a value in range [6, 10]
     *         // cannot be less than or equal to variable <i>b</i> with range [3, 5]
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with.
     * @return the refined {@code Range}.
     */
    public Range refineLessThanEq(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        long resultTo = Math.min(to, right.to);
        if (from <= resultTo) {
            return new Range(from, resultTo);
        } else {
            return NOTHING;
        }
    }

    /**
     * Determines a refined range of a variable that fall within this {@code Range} under the
     * condition that this variable is greater than another variable that fall within the specified
     * {@code Range}. This is used for calculating the control flow refined result of the &gt;
     * operator. i.e.
     *
     * <pre>
     * <code>
     *     {@literal @}IntRange(from = 0, to = 10) int a;
     *     {@literal @}IntRange(from = 3, to = 5) int b;
     *     ...
     *     if (a &gt; b) {
     *         // range of <i>a</i> is now refined to [6, 10] because a value in range [0, 5]
     *         // cannot be greater than variable <i>b</i> with range [3, 5]
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with.
     * @return the refined {@code Range}.
     */
    public Range refineGreaterThan(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (right.from == Long.MAX_VALUE) {
            return NOTHING;
        }

        long resultFrom = Math.max(from, right.from + 1);
        if (resultFrom <= to) {
            return new Range(resultFrom, to);
        } else {
            return NOTHING;
        }
    }

    /**
     * Determines a refined range of a variable that fall within this {@code Range} under the
     * condition that this variable is greater than or equal to another variable that fall within
     * the specified {@code Range}. This is used for calculating the control flow refined result of
     * the &gt;= operator. i.e.
     *
     * <pre>
     * <code>
     *     {@literal @}IntRange(from = 0, to = 10) int a;
     *     {@literal @}IntRange(from = 3, to = 5) int b;
     *     ...
     *     if (a &gt;= b) {
     *         // range of <i>a</i> is now refined to [5, 10] because a value in range [0, 4]
     *         // cannot be greater than or equal to variable <i>b</i> with range [3, 5]
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with.
     * @return the refined {@code Range}.
     */
    public Range refineGreaterThanEq(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        long resultFrom = Math.max(from, right.from);
        if (resultFrom <= to) {
            return new Range(resultFrom, to);
        } else {
            return NOTHING;
        }
    }

    /**
     * Determines a refined range of a variable that fall within this {@code Range} under the
     * condition that this variable is equal to another variable that fall within the specified
     * {@code Range}. This is used for calculating the control flow refined result of the ==
     * operator. i.e.
     *
     * <pre>
     * <code>
     *     {@literal @}IntRange(from = 0, to = 10) int a;
     *     {@literal @}IntRange(from = 3, to = 15) int b;
     *     ...
     *     if (a == b) {
     *         // range of <i>a</i> is now refined to [3. 10] because a value in range [0, 2]
     *         // cannot be equal to variable <i>b</i> with range [3, 15]
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with.
     * @return the refined {@code Range}.
     */
    public Range refineEqualTo(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        long resultFrom = Math.max(from, right.from);
        long resultTo = Math.min(to, right.to);
        if (resultFrom <= resultTo) {
            return new Range(resultFrom, resultTo);
        } else {
            return NOTHING;
        }
    }

    /**
     * Returns the number of possible values enclosed by this range. To prevent overflow, we use
     * BigInteger for calculation and return a BitInteger.
     *
     * @return the number of possible values enclosed by this range
     */
    public BigInteger numberOfPossibleValues() {
        return BigInteger.valueOf(to).subtract(BigInteger.valueOf(from)).add(BigInteger.valueOf(1));
    }

    /**
     * Determines if the range is wider than a given value, i.e., if the number of possible values
     * enclosed by this range is more than the given value.
     *
     * @param value the value to compare with
     * @return true if wider than the given value
     */
    public boolean isWiderThan(long value) {
        return numberOfPossibleValues().compareTo(BigInteger.valueOf(value)) == 1;
    }

    /**
     * Determines if this range is completely contained in the scope of the Integer type.
     *
     * @return true if the range is completely contained in the scope of the Integer type.
     */
    public boolean isWithinInteger() {
        return from >= Integer.MIN_VALUE && to <= Integer.MAX_VALUE;
    }

    /**
     * Converts a range with BigInteger type bounds to a range with Long type bounds.
     *
     * <p>If the BigInteger bounds are out of the Long type scope, convert the bounds to Long type
     * as if it is overflow, e.g., Long.MAX_VALUE + 1 is converted to Long.MIN_VALUE
     *
     * <p>If the BigInteger range is too wide, i.e., wider than the full range of Long class, return
     * EVERYTHING
     *
     * @param bigFrom the lower bound of the BigInteger range
     * @param bigTo the upper bound of the BigInteger range
     * @return a range with Long type bounds converted from the BigInteger range
     */
    private static Range bigRange2LongRange(BigInteger bigFrom, BigInteger bigTo) {
        if (bigTo.subtract(bigFrom)
                        .compareTo(BigInteger.valueOf(Long.MAX_VALUE - Long.MIN_VALUE + 1))
                == 1) {
            return EVERYTHING;
        } else {
            long resultFrom = bigFrom.longValue();
            long resultTo = bigTo.longValue();
            if (resultFrom <= resultTo) {
                return new Range(resultFrom, resultTo);
            } else {
                return EVERYTHING;
            }
        }
    }
}
