package org.checkerframework.common.value.util;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.dataflow.util.HashCodeUtils;

/**
 * The Range class models a 64-bit two's-complement integral interval, such as all integers between
 * 1 and 10, inclusive. Ranges are immutable.
 *
 * @author JasonMrX
 */
public class Range {

    /** The lower bound of the interval, inclusive. */
    public final long from;

    /** The upper bound of the interval, inclusive. */
    public final long to;

    /**
     * Should ranges take overflow into account or ignore it?
     *
     * <p>Any checker that uses this library should be sure to set this field. By default, this
     * field is set to false (meaning overflow is taken into account), but a previous checker might
     * have set it to true.
     *
     * <p>A static field is used because passing an instance field throughout the class (and at all
     * of its use cases) results in the code being unacceptably bloated.
     */
    public static boolean IGNORE_OVERFLOW = false;

    /** A range containing all possible 64-bit values. */
    public static final Range EVERYTHING = new Range(Long.MIN_VALUE, Long.MAX_VALUE);

    /** A range containing all possible 32-bit values. */
    public static final Range INT_EVERYTHING = new Range(Integer.MIN_VALUE, Integer.MAX_VALUE);

    /** A range containing all possible 16-bit values. */
    public static final Range SHORT_EVERYTHING = new Range(Short.MIN_VALUE, Short.MAX_VALUE);

    /** A range containing all possible 8-bit values. */
    public static final Range BYTE_EVERYTHING = new Range(Byte.MIN_VALUE, Byte.MAX_VALUE);

    /** The empty range. */
    public static final Range NOTHING = new Range();

    /**
     * Constructs a range with its bounds specified by two parameters, {@code from} and {@code to}.
     *
     * @param from the lower bound (inclusive)
     * @param to the upper bound (inclusive)
     */
    public Range(long from, long to) {
        if (!(from <= to)) {
            throw new IllegalArgumentException(String.format("Invalid Range: %s %s", from, to));
        }
        this.from = from;
        this.to = to;
    }

    /** Creates the singleton empty range. */
    private Range() {
        this.from = Long.MAX_VALUE;
        this.to = Long.MIN_VALUE;
    }

    /**
     * Returns a range with its bounds specified by two parameters, {@code from} and {@code to}. If
     * {@code from} is greater than {@code to}, returns {@link #NOTHING}.
     *
     * @param from the lower bound (inclusive)
     * @param to the upper bound (inclusive)
     */
    private Range createRangeOrNothing(long from, long to) {
        if (from <= to) {
            return new Range(from, to);
        } else {
            return NOTHING;
        }
    }

    @Override
    public String toString() {
        if (this.isNothing()) {
            return "[]";
        } else {
            return String.format("[%s..%s]", from, to);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Range) {
            Range range = (Range) obj;
            return from == range.from && to == range.to;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(from, to);
    }

    /** Return true if this range contains every {@code long} value. */
    public boolean isLongEverything() {
        return from == Long.MIN_VALUE && to == Long.MAX_VALUE;
    }

    /** Return true if this range contains every {@code int} value. */
    public boolean isIntEverything() {
        return from == Integer.MIN_VALUE && to == Integer.MAX_VALUE;
    }

    /** Return true if this range contains every {@code short} value. */
    public boolean isShortEverything() {
        return from == Short.MIN_VALUE && to == Short.MAX_VALUE;
    }

    /** Return true if this range contains every {@code byte} value. */
    public boolean isByteEverything() {
        return from == Byte.MIN_VALUE && to == Byte.MAX_VALUE;
    }

    /** Return true if this range contains no values. */
    public boolean isNothing() {
        return this == NOTHING;
    }

    /** The number of values representable in 32 bits: 2^32 or 1&lt;&lt;32. */
    private static long integerWidth = (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE + 1;

    /**
     * Converts a this range to a 32-bit integral range.
     *
     * <p>If {@link #IGNORE_OVERFLOW} is true and one of the bounds is outside the Integer range,
     * then that bound is set to the bound of the Integer range.
     *
     * <p>If {@link #IGNORE_OVERFLOW} is false and this range is too wide, i.e., wider than the full
     * range of the Integer class, return INT_EVERYTHING.
     *
     * <p>If {@link #IGNORE_OVERFLOW} is false and the bounds of this range are not representable as
     * 32-bit integers, convert the bounds to Integer type in accordance with Java overflow rules,
     * e.g., Integer.MAX_VALUE + 1 is converted to Integer.MIN_VALUE.
     */
    public Range intRange() {
        if (this.isNothing()) {
            return this;
        }
        if (IGNORE_OVERFLOW) {
            return new Range(Math.max(from, Integer.MIN_VALUE), Math.min(to, Integer.MAX_VALUE));
        }
        if (this.isWiderThan(integerWidth)) {
            return INT_EVERYTHING;
        }
        int intFrom = (int) this.from;
        int intTo = (int) this.to;
        if (intFrom <= intTo) {
            return new Range(intFrom, intTo);
        }
        return INT_EVERYTHING;
    }

    /** The number of values representable in 16 bits: 2^16 or 1&lt;&lt;16. */
    private static long shortWidth = Short.MAX_VALUE - Short.MIN_VALUE + 1;

    /**
     * Converts a this range to a 16-bit short range.
     *
     * <p>If {@link #IGNORE_OVERFLOW} is true and one of the bounds is outside the Short range, then
     * that bound is set to the bound of the Short range.
     *
     * <p>If {@link #IGNORE_OVERFLOW} is false and this range is too wide, i.e., wider than the full
     * range of the Short class, return SHORT_EVERYTHING.
     *
     * <p>If {@link #IGNORE_OVERFLOW} is false and the bounds of this range are not representable as
     * 16-bit integers, convert the bounds to Integer type in accordance with Java overflow rules,
     * e.g., Short.MAX_VALUE + 1 is converted to Short.MIN_VALUE.
     */
    public Range shortRange() {
        if (this.isNothing()) {
            return this;
        }
        if (IGNORE_OVERFLOW) {
            return new Range(Math.max(from, Short.MIN_VALUE), Math.min(to, Short.MAX_VALUE));
        }
        if (this.isWiderThan(shortWidth)) {
            // short is be promoted to int before the operation so no need for explicit casting
            return SHORT_EVERYTHING;
        }
        short shortFrom = (short) this.from;
        short shortTo = (short) this.to;
        if (shortFrom <= shortTo) {
            return new Range(shortFrom, shortTo);
        }
        return SHORT_EVERYTHING;
    }

    /** The number of values representable in 8 bits: 2^8 or 1&lt;&lt;8. */
    private static long byteWidth = Byte.MAX_VALUE - Byte.MIN_VALUE + 1;

    /**
     * Converts a this range to a 8-bit byte range.
     *
     * <p>If {@link #IGNORE_OVERFLOW} is true and one of the bounds is outside the Byte range, then
     * that bound is set to the bound of the Byte range.
     *
     * <p>If {@link #IGNORE_OVERFLOW} is false and this range is too wide, i.e., wider than the full
     * range of the Byte class, return BYTE_EVERYTHING.
     *
     * <p>If {@link #IGNORE_OVERFLOW} is false and the bounds of this range are not representable as
     * 8-bit integers, convert the bounds to Integer type in accordance with Java overflow rules,
     * e.g., Byte.MAX_VALUE + 1 is converted to Byte.MIN_VALUE.
     */
    public Range byteRange() {
        if (this.isNothing()) {
            return this;
        }
        if (IGNORE_OVERFLOW) {
            return new Range(Math.max(from, Byte.MIN_VALUE), Math.min(to, Byte.MAX_VALUE));
        }
        if (this.isWiderThan(byteWidth)) {
            // byte is be promoted to int before the operation so no need for explicit casting
            return BYTE_EVERYTHING;
        }
        byte byteFrom = (byte) this.from;
        byte byteTo = (byte) this.to;
        if (byteFrom <= byteTo) {
            return new Range(byteFrom, byteTo);
        }
        return BYTE_EVERYTHING;
    }

    /** Returns true if the element is contained in this range. */
    public boolean contains(long element) {
        return from <= element && element <= to;
    }

    /** Returns true if the element is contained in this range. */
    public boolean contains(Range other) {
        return from <= other.from && other.to <= to;
    }

    /**
     * Returns the smallest range that includes all values contained in either of the two ranges. We
     * call this the union of two ranges.
     *
     * @param right a range to union with this range
     * @return a range resulting from the union of the specified range and this range
     */
    public Range union(Range right) {
        if (this.isNothing()) {
            return right;
        } else if (right.isNothing()) {
            return this;
        }

        long resultFrom = Math.min(from, right.from);
        long resultTo = Math.max(to, right.to);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns the smallest range that includes all values contained in both of the two ranges. We
     * call this the intersection of two ranges. If there is no overlap between the two ranges,
     * returns an empty range.
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
        return createRangeOrNothing(resultFrom, resultTo);
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

        if (this.isWithinHalfLong() && right.isWithinHalfLong()) {
            // This bound is adequate to guarantee no overflow when using long to evaluate
            long resultFrom = from + right.from;
            long resultTo = to + right.to;
            return new Range(resultFrom, resultTo);
        } else {
            BigInteger bigFrom = BigInteger.valueOf(from).add(BigInteger.valueOf(right.from));
            BigInteger bigTo = BigInteger.valueOf(to).add(BigInteger.valueOf(right.to));
            return bigRangeToLongRange(bigFrom, bigTo);
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

        if (this.isWithinHalfLong() && right.isWithinHalfLong()) {
            // This bound is adequate to guarantee no overflow when using long to evaluate
            long resultFrom = from - right.to;
            long resultTo = to - right.from;
            return new Range(resultFrom, resultTo);
        } else {
            BigInteger bigFrom = BigInteger.valueOf(from).subtract(BigInteger.valueOf(right.to));
            BigInteger bigTo = BigInteger.valueOf(to).subtract(BigInteger.valueOf(right.from));
            return bigRangeToLongRange(bigFrom, bigTo);
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

        // These bounds are adequate:  Integer.MAX_VALUE^2 is still a bit less than Long.MAX_VALUE.
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
            return bigRangeToLongRange(bigFrom, bigTo);
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
        if (right.from == 0 && right.to == 0) {
            return NOTHING;
        }
        // Special cases that involve overflow.
        // The only overflow in integer division is Long.MIN_VALUE / -1 == Long.MIN_VALUE.
        if (from == Long.MIN_VALUE && right.contains(-1)) {
            // The values in the right range are all negative because right does not contain 0 but
            // does contain 1.
            if (from != to) {
                // Special case 1:
                // This range contains Long.MIN_VALUE and Long.MIN_VALUE + 1, which makes the
                // result range EVERYTHING.
                return EVERYTHING;
            } else if (right.from != right.to) {
                // Special case 2:
                // This range contains only Long.MIN_VALUE, and the right range contains at least -1
                // and -2. The result range is from Long.MIN_VALUE to Long.MIN_VALUE / -2.
                return new Range(Long.MIN_VALUE, Long.MIN_VALUE / -2);
            } else {
                // Special case 3:
                // This range contains only Long.MIN_VALUE, and right contains only -1.
                return new Range(Long.MIN_VALUE, Long.MIN_VALUE);
            }
        }
        // We needn't worry about the overflow issue starting from here.

        // There are 9 different cases:
        // (note: pos=positive, neg=negative, unk=unknown sign, np=non-positive, nn=non-negative)
        long resultFrom;
        long resultTo;
        if (from > 0) { // this range is positive
            if (right.from >= 0) {
                // 1. right: nn
                resultFrom = from / Math.max(right.to, 1);
                resultTo = to / Math.max(right.from, 1);
            } else if (right.to <= 0) {
                // 2. right: np
                resultFrom = to / Math.min(right.to, -1);
                resultTo = from / Math.min(right.from, -1);
            } else {
                // 3. right: unk; values include -1 and 1
                resultFrom = -to;
                resultTo = to;
            }
        } else if (to < 0) { // this range is negative
            if (right.from >= 0) {
                // 4. right: nn
                resultFrom = from / Math.max(right.from, 1);
                resultTo = to / Math.max(right.to, 1);
            } else if (right.to <= 0) {
                // 5. right: np
                resultFrom = to / Math.min(right.from, -1);
                resultTo = from / Math.min(right.to, -1);
            } else {
                // 6. right: unk; values include -1 and 1
                resultFrom = from;
                resultTo = -from;
            }
        } else { // this range spans both signs
            if (right.from >= 0) {
                // 7. right: nn
                resultFrom = from / Math.max(right.from, 1);
                resultTo = to / Math.max(right.from, 1);
            } else if (right.to <= 0) {
                // 8. right: np
                resultFrom = to / Math.min(right.to, -1);
                resultTo = from / Math.min(right.to, -1);
            } else {
                // 9. right: unk; values include -1 and 1
                resultFrom = Math.min(from, -to);
                resultTo = Math.max(-from, to);
            }
        }
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns a range that includes all possible values of the remainder of dividing an arbitrary
     * value in this range by an arbitrary value in the specified range.
     *
     * <p>In the current implementation, the result might not be the smallest range that includes
     * all the possible values.
     *
     * @param right the specified range by which this range is divided
     * @return the range of the remainder of dividing this range by the specified range
     */
    public Range remainder(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }
        if (right.from == 0 && right.to == 0) {
            return NOTHING;
        }
        // Special cases that would cause overflow if we use the general method below
        if (right.from == Long.MIN_VALUE) {
            Range range;
            // The value Long.MIN_VALUE as a divisor needs special handling as follows:
            if (from == Long.MIN_VALUE) {
                if (to == Long.MIN_VALUE) {
                    // This range only contains Long.MIN_VALUE, so the result range is {0}.
                    range = new Range(0, 0);
                } else { // (to > Long.MIN_VALUE)
                    // When this range contains Long.MIN_VALUE, which would have a remainder of 0 if
                    // divided by Long.MIN_VALUE, the result range is {0} unioned with [from + 1, to]
                    range = (new Range(from + 1, to)).union(new Range(0, 0));
                }
            } else { // (from > Long.MIN_VALUE)
                // When this range doesn't contain Long.MIN_VALUE, the remainder of each value
                // in this range divided by Long.MIN_VALUE is this value itself. Therefore the
                // result range is this range itself.
                range = this;
            }
            // If right.to > Long.MIN_VALUE, union the previous result with the result of range
            // [right.from + 1, right.to] divided by this range, which can be calculated using
            // the general method (see below)
            if (right.to > Long.MIN_VALUE) {
                Range rangeAdditional = this.remainder(new Range(right.from + 1, right.to));
                range = range.union(rangeAdditional);
            }
            return range;
        }
        // General method:
        // Calculate range1: the result range of this range divided by EVERYTHING. For example,
        // if this range is [3, 5], then the result range would be [0, 5]. If this range is [-3, 4],
        // then the result range would be [-3, 4]. In general, the result range is {0} union with
        // this range excluding the value Long.MIN_VALUE.
        Range range1 =
                (new Range(Math.max(Long.MIN_VALUE + 1, from), Math.max(Long.MIN_VALUE + 1, to)))
                        .union(new Range(0, 0));
        // Calculate range2: the result range of range EVERYTHING divided by the right range. For
        // example, if the right range is [-5, 3], then the result range would be [-4, 4]. If the
        // right range is [3, 6], then the result range would be [-5, 5]. In general, the result
        // range is calculated as following:
        long maxAbsolute = Math.max(Math.abs(right.from), Math.abs(right.to));
        Range range2 = new Range(-maxAbsolute + 1, maxAbsolute - 1);
        // Since range1 and range2 are both super sets of the minimal result range, we return the
        // intersection of range1 and range2, which is correct (super set) and precise enough.
        return range1.intersect(range2);
    }

    /**
     * Returns a range that includes all possible values resulting from left shifting an arbitrary
     * value in this range by an arbitrary number of bits in the specified range. We call this the
     * left shift of a range.
     *
     * @param right the range of bits by which this range is left shifted
     * @return the range resulting from left shifting this range by the specified range
     */
    public Range shiftLeft(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        // Shifting operations in Java are depending on the type of the left-hand operand:
        // If the left-hand operand is int  type, only the 5 lowest-order bits of the right-hand operand are used
        // If the left-hand operand is long type, only the 6 lowest-order bits of the right-hand operand are used
        // For example, while 1 << -1== 1 << 31, 1L << -1 == 1L << 63.
        // For ths reason, we restrict the shift-bits to analyze in [0. 31] and give up the analysis when out of this range.
        //
        // Other possible solutions:
        // 1. create different methods for int type and long type and use them accordingly
        // 2. add an additional boolean parameter to indicate the type of the left-hand operand
        //
        // see http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19 for more detail.
        if (right.isWithin(0, 31)) {
            if (this.isWithinInteger()) {
                // This bound is adequate to guarantee no overflow when using long to evaluate
                long resultFrom = from << (from >= 0 ? right.from : right.to);
                long resultTo = to << (to >= 0 ? right.to : right.from);
                return new Range(resultFrom, resultTo);
            } else {
                BigInteger bigFrom =
                        BigInteger.valueOf(from)
                                .shiftLeft(from >= 0 ? (int) right.from : (int) right.to);
                BigInteger bigTo =
                        BigInteger.valueOf(to)
                                .shiftLeft(to >= 0 ? (int) right.to : (int) right.from);
                return bigRangeToLongRange(bigFrom, bigTo);
            }
        } else {
            // In other cases, we give up on the calculation and return EVERYTHING (rare in practice).
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

        if (this.isWithinInteger() && right.isWithin(0, 31)) {
            // This bound is adequate to guarantee no overflow when using long to evaluate
            long resultFrom = from >> (from >= 0 ? right.to : right.from);
            long resultTo = to >> (to >= 0 ? right.from : right.to);
            return new Range(resultFrom, resultTo);
        } else {
            // Signed shift right operation for long type cannot be simulated with BigInteger.
            // Give up on the calculation and return EVERYTHING instead.
            return EVERYTHING;
        }
    }

    /** We give up the analysis for unsigned shift right operation */
    public Range unsignedShiftRight(Range right) {
        return EVERYTHING;
    }

    /** We give up the analysis for bitwise AND operation */
    public Range bitwiseAnd(Range right) {
        return EVERYTHING;
    }

    /** We give up the analysis for bitwise OR operation */
    public Range bitwiseOr(Range right) {
        return EVERYTHING;
    }

    /** We give up the analysis for bitwise XOR operation */
    public Range bitwiseXor(Range right) {
        return EVERYTHING;
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
     * Returns the range of a variable that falls within this range after applying the unary minus
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
     * Returns the range of a variable that falls within this range after applying the bitwise
     * complement operation.
     *
     * @return the resulting range of applying bitwise complement on an arbitrary value in this
     *     range
     */
    public Range bitwiseComplement() {
        if (this.isNothing()) {
            return NOTHING;
        }

        return new Range(~to, ~from);
    }

    /**
     * Refines this range to reflect that some value in it can be less than a value in the given
     * range. This is used for calculating the control-flow-refined result of the &lt; operator. For
     * example:
     *
     * <pre>
     * <code>
     *    {@literal @}IntRange(from = 0, to = 10) int a;
     *    {@literal @}IntRange(from = 3, to = 7) int b;
     *     ...
     *     if (a &lt; b) {
     *         // range of <i>a</i> is now refined to [0, 6] because a value in range [7, 10]
     *         // cannot be smaller than variable <i>b</i> with range [3, 7].
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * Use the {@link #refineGreaterThanEq(Range)} method if you are also interested in refining the
     * range of {@code b} in the code above.
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range refineLessThan(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (right.to == Long.MIN_VALUE) {
            return NOTHING;
        }

        long resultTo = Math.min(to, right.to - 1);
        return createRangeOrNothing(from, resultTo);
    }

    /**
     * Refines this range to reflect that some value in it can be less than or equal to a value in
     * the given range. This is used for calculating the control-flow-refined result of the &lt;=
     * operator. For example:
     *
     * <pre>
     * <code>
     *    {@literal @}IntRange(from = 0, to = 10) int a;
     *    {@literal @}IntRange(from = 3, to = 7) int b;
     *     ...
     *     if (a &lt;= b) {
     *         // range of <i>a</i> is now refined to [0, 7] because a value in range [8, 10]
     *         // cannot be less than or equal to variable <i>b</i> with range [3, 7].
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * Use the {@link #refineGreaterThan(Range)} method if you are also interested in refining the
     * range of {@code b} in the code above.
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range refineLessThanEq(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        long resultTo = Math.min(to, right.to);
        return createRangeOrNothing(from, resultTo);
    }

    /**
     * Refines this range to reflect that some value in it can be greater than a value in the given
     * range. This is used for calculating the control-flow-refined result of the &gt; operator. For
     * example:
     *
     * <pre>
     * <code>
     *    {@literal @}IntRange(from = 0, to = 10) int a;
     *    {@literal @}IntRange(from = 3, to = 7) int b;
     *     ...
     *     if (a &gt; b) {
     *         // range of <i>a</i> is now refined to [4, 10] because a value in range [0, 3]
     *         // cannot be greater than variable <i>b</i> with range [3, 7].
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * Use the {@link #refineLessThanEq(Range)} method if you are also interested in refining the
     * range of {@code b} in the code above.
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range refineGreaterThan(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        if (right.from == Long.MAX_VALUE) {
            return NOTHING;
        }

        long resultFrom = Math.max(from, right.from + 1);
        return createRangeOrNothing(resultFrom, to);
    }

    /**
     * Refines this range to reflect that some value in it can be greater than or equal to a value
     * in the given range. This is used for calculating the control-flow-refined result of the &gt;=
     * operator. For example:
     *
     * <pre>
     * <code>
     *    {@literal @}IntRange(from = 0, to = 10) int a;
     *    {@literal @}IntRange(from = 3, to = 7) int b;
     *     ...
     *     if (a &gt;= b) {
     *         // range of <i>a</i> is now refined to [3, 10] because a value in range [0, 2]
     *         // cannot be greater than or equal to variable <i>b</i> with range [3, 7].
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * Use the {@link #refineLessThan(Range)} method if you are also interested in refining the
     * range of {@code b} in the code above.
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range refineGreaterThanEq(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        long resultFrom = Math.max(from, right.from);
        return createRangeOrNothing(resultFrom, to);
    }

    /**
     * Refines this range to reflect that some value in it can be equal to a value in the given
     * range. This is used for calculating the control-flow-refined result of the == operator. For
     * example:
     *
     * <pre>
     * <code>
     *    {@literal @}IntRange(from = 0, to = 10) int a;
     *    {@literal @}IntRange(from = 3, to = 15) int b;
     *     ...
     *     if (a == b) {
     *         // range of <i>a</i> is now refined to [3, 10] because a value in range [0, 2]
     *         // cannot be equal to variable <i>b</i> with range [3, 15].
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range refineEqualTo(Range right) {
        return this.intersect(right);
    }

    /**
     * Refines this range to reflect that some value in it must not be equal to a value in the given
     * range. This only changes the range if the given range (right) contains exactly one integer,
     * and that integer is one of the bounds of this range. This is used for calculating the
     * control-flow-refined result of the != operator. For example:
     *
     * <pre>
     * <code>
     *    {@literal @}IntRange(from = 0, to = 10) int a;
     *    {@literal @}IntRange(from = 0, to = 0) int b;
     *     ...
     *     if (a != b) {
     *         // range of <i>a</i> is now refined to [1, 10] because it cannot
     *         // be zero.
     *         ...
     *     }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range refineNotEqualTo(Range right) {
        if (right.to == right.from) {
            if (this.to == right.to) {
                return new Range(this.from, this.to - 1);
            } else if (this.from == right.from) {
                return new Range(this.from + 1, this.to);
            }
        }
        return this;
    }

    /**
     * Determines if the range is wider than a given value, i.e., if the number of possible values
     * enclosed by this range is more than the given value.
     *
     * @param value the value to compare with
     * @return true if wider than the given value
     */
    public boolean isWiderThan(long value) {
        if (this.isWithin((Long.MIN_VALUE >> 1) + 1, Long.MAX_VALUE >> 1)) {
            // This bound is adequate to guarantee no overflow when using long to evaluate.
            // Long.MIN_VALUE >> 1 + 1 = -4611686018427387903
            // Long.MAX_VALUE >> 1 = 4611686018427387903
            return to - from + 1 > value;
        } else {
            return BigInteger.valueOf(to)
                            .subtract(BigInteger.valueOf(from))
                            .add(BigInteger.ONE)
                            .compareTo(BigInteger.valueOf(value))
                    == 1;
        }
    }

    /**
     * Determines if this range is completely contained in the range specified by the given lower
     * bound and upper bound.
     */
    public boolean isWithin(long lb, long ub) {
        return from >= lb && to <= ub;
    }

    /**
     * Determines if this range is completely contained in the range that is of half length of the
     * Long type and centered with 0.
     */
    private boolean isWithinHalfLong() {
        return isWithin(Long.MIN_VALUE >> 1, Long.MAX_VALUE >> 1);
    }

    /** Determines if this range is completely contained in the scope of the Integer type. */
    private boolean isWithinInteger() {
        return isWithin(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static final BigInteger longWidth =
            BigInteger.valueOf(Long.MAX_VALUE)
                    .subtract(BigInteger.valueOf(Long.MIN_VALUE))
                    .add(BigInteger.ONE);

    /**
     * Converts a range with BigInteger type bounds to a range with Long type bounds.
     *
     * <p>If the BigInteger range is too wide, i.e., wider than the full range of the Long class,
     * return EVERYTHING.
     *
     * <p>If one of the BigInteger bounds is out of Long's range and {@link #IGNORE_OVERFLOW} is
     * false, convert the bounds to Long type in accordance with Java overflow rules, e.g.,
     * Long.MAX_VALUE + 1 is converted to Long.MIN_VALUE.
     *
     * <p>If one of the BigInteger bounds is out of Long's range and {@link #IGNORE_OVERFLOW} is
     * true, convert the bound that is outside Long's range to max/min value of a Long.
     *
     * @param bigFrom the lower bound of the BigInteger range
     * @param bigTo the upper bound of the BigInteger range
     * @return a range with Long type bounds converted from the BigInteger range
     */
    private Range bigRangeToLongRange(BigInteger bigFrom, BigInteger bigTo) {
        BigInteger numValues = bigTo.subtract(bigFrom).add(BigInteger.ONE);
        long resultFrom;
        long resultTo;
        if (IGNORE_OVERFLOW) {
            BigInteger longMin = BigInteger.valueOf(Long.MIN_VALUE);
            resultFrom = bigFrom.max(longMin).longValue();
            BigInteger longMax = BigInteger.valueOf(Long.MAX_VALUE);
            resultTo = bigTo.min(longMax).longValue();
        } else {
            if (numValues.compareTo(longWidth) > 0) {
                return EVERYTHING;
            } else {
                resultFrom = bigFrom.longValue();
                resultTo = bigTo.longValue();
            }
        }

        if (resultFrom <= resultTo) {
            return new Range(resultFrom, resultTo);
        } else {
            return EVERYTHING;
        }
    }
}
