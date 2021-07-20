package org.checkerframework.common.value.util;

import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.type.TypeKind;

/**
 * The Range class models a 64-bit two's-complement integral interval, such as all integers between
 * 1 and 10, inclusive.
 *
 * <p>{@code Range} is immutable.
 */
public class Range {

    /** The lower bound of the interval, inclusive. */
    public final long from;

    /** The upper bound of the interval, inclusive. */
    public final long to;

    /**
     * Should ranges take overflow into account or ignore it?
     *
     * <ul>
     *   <li>If {@code ignoreOverflow} is true, then operations that would result in more than the
     *       max value are clipped to the max value (and similarly for the min).
     *   <li>If {@code ignoreOverflow} is false, then operations that would result in more than the
     *       max wrap around according to the rules of twos-complement arithmetic and produce a
     *       smaller value (and similarly for the min).
     * </ul>
     *
     * <p>Any checker that uses this library should set this field. By default, this field is set to
     * false (meaning overflow is taken into account), but a previous checker might have set it to
     * true.
     *
     * <p>A static field is used because passing an instance field throughout the class bloats the
     * code.
     */
    public static boolean ignoreOverflow = false;

    /** A range containing all possible 64-bit values. */
    public static final Range LONG_EVERYTHING = create(Long.MIN_VALUE, Long.MAX_VALUE);

    /** A range containing all possible 32-bit values. */
    public static final Range INT_EVERYTHING = create(Integer.MIN_VALUE, Integer.MAX_VALUE);

    /** A range containing all possible 16-bit values. */
    public static final Range SHORT_EVERYTHING = create(Short.MIN_VALUE, Short.MAX_VALUE);

    /** A range containing all possible char values. */
    public static final Range CHAR_EVERYTHING = create(Character.MIN_VALUE, Character.MAX_VALUE);

    /** A range containing all possible 8-bit values. */
    public static final Range BYTE_EVERYTHING = create(Byte.MIN_VALUE, Byte.MAX_VALUE);

    /** The empty range. This is the only Range object that contains nothing */
    @SuppressWarnings(
            "interning:assignment.type.incompatible") // no other constructor call makes this
    public static final @InternedDistinct Range NOTHING = new Range(Long.MAX_VALUE, Long.MIN_VALUE);

    /** An alias to the range containing all possible 64-bit values. */
    public static final Range EVERYTHING = LONG_EVERYTHING;

    /**
     * Constructs a range with its bounds specified by two parameters, {@code from} and {@code to}.
     *
     * <p>This is a private constructor that does no validation of arguments, so special instances
     * (e.g., {@link #NOTHING}) can be created through it.
     *
     * @param from the lower bound (inclusive)
     * @param to the upper bound (inclusive)
     */
    private Range(long from, long to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Constructs a range with its bounds specified by two parameters, {@code from} and {@code to}.
     * Requires {@code from <= to}.
     *
     * @param from the lower bound (inclusive)
     * @param to the upper bound (inclusive)
     * @return the Range [from..to]
     */
    public static Range create(long from, long to) {
        if (!(from <= to)) {
            throw new IllegalArgumentException(String.format("Invalid Range: %s %s", from, to));
        }
        return new Range(from, to);
    }

    /**
     * Create a Range from a collection of Numbers.
     *
     * @param values collection whose min and max values will be used as the range's from and to
     *     values
     * @return a range that encompasses all the argument's values ({@link #NOTHING} if the argument
     *     is an empty collection)
     */
    public static Range create(Collection<? extends Number> values) {
        if (values.isEmpty()) {
            return NOTHING;
        }
        long min = values.iterator().next().longValue();
        long max = min;
        for (Number value : values) {
            long current = value.longValue();
            if (min > current) min = current;
            if (max < current) max = current;
        }
        return create(min, max);
    }

    /**
     * Returns a Range representing all possible values for the given primitive type.
     *
     * @param typeKind one of INT, SHORT, BYTE, CHAR, or LONG
     * @return the range for the given primitive type
     */
    public static Range create(TypeKind typeKind) {
        switch (typeKind) {
            case INT:
                return INT_EVERYTHING;
            case SHORT:
                return SHORT_EVERYTHING;
            case BYTE:
                return BYTE_EVERYTHING;
            case CHAR:
                return CHAR_EVERYTHING;
            case LONG:
                return LONG_EVERYTHING;
            default:
                throw new IllegalArgumentException(
                        "Invalid TypeKind for Range: expected INT, SHORT, BYTE, CHAR, or LONG, got "
                                + typeKind);
        }
    }

    /** Long.MIN_VALUE, as a BigInteger. */
    private static final BigInteger BIG_LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
    /** Long.MAX_VALUE, as a BigInteger. */
    private static final BigInteger BIG_LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);
    /** The number of Long values, as a BigInteger. */
    private static final BigInteger BIG_LONG_WIDTH =
            BIG_LONG_MAX_VALUE.subtract(BIG_LONG_MIN_VALUE).add(BigInteger.ONE);

    /**
     * Creates a range using BigInteger type bounds.
     *
     * <p>If the BigInteger range is wider than the full range of the Long class, return EVERYTHING.
     *
     * <p>If one of the BigInteger bounds is out of Long's range and {@link #ignoreOverflow} is
     * false, convert the bounds to Long type in accordance with Java twos-complement overflow
     * rules, e.g., Long.MAX_VALUE + 1 is converted to Long.MIN_VALUE.
     *
     * <p>If one of the BigInteger bounds is out of Long's range and {@link #ignoreOverflow} is
     * true, convert the bound that is outside Long's range to max/min value of a Long.
     *
     * @param bigFrom the lower bound of the BigInteger range
     * @param bigTo the upper bound of the BigInteger range
     * @return a range with Long type bounds converted from the BigInteger range
     */
    private static Range create(BigInteger bigFrom, BigInteger bigTo) {
        if (ignoreOverflow) {
            bigFrom = bigFrom.max(BIG_LONG_MIN_VALUE);
            bigTo = bigTo.min(BIG_LONG_MAX_VALUE);
        } else {
            BigInteger bigWidth = bigTo.subtract(bigFrom).add(BigInteger.ONE);
            if (bigWidth.compareTo(BIG_LONG_WIDTH) > 0) {
                return EVERYTHING;
            }
        }
        long longFrom = bigFrom.longValue();
        long longTo = bigTo.longValue();
        return createOrElse(longFrom, longTo, EVERYTHING);
    }

    /**
     * Creates a Range if {@code from<=to}; otherwise returns the given Range value.
     *
     * @param from lower bound for the range
     * @param to upper bound for the range
     * @param alternate what to return if {@code from > to}
     * @return a new Range [from..to], or {@code alternate}
     */
    private static Range createOrElse(long from, long to, Range alternate) {
        if (from <= to) {
            return new Range(from, to);
        } else {
            return alternate;
        }
    }

    /**
     * Returns a range with its bounds specified by two parameters, {@code from} and {@code to}. If
     * {@code from} is greater than {@code to}, returns {@link #NOTHING}.
     *
     * @param from the lower bound (inclusive)
     * @param to the upper bound (inclusive)
     * @return newly-created Range or NOTHING
     */
    private static Range createOrNothing(long from, long to) {
        return createOrElse(from, to, NOTHING);
    }

    /**
     * Returns the number of values in this range.
     *
     * @return how many values are in the range
     */
    private long width() {
        return to - from + 1;
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
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Range) {
            return equalsRange((Range) obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    /**
     * Compare two ranges in a type safe manner for equality without incurring the cost of an
     * instanceof check such as equals(Object) does.
     *
     * @param range to compare against
     * @return true for ranges that match from and to respectively
     */
    private boolean equalsRange(Range range) {
        return from == range.from && to == range.to;
    }

    /** Return true if this range contains every {@code long} value. */
    public boolean isLongEverything() {
        return equalsRange(LONG_EVERYTHING);
    }

    /** Return true if this range contains every {@code int} value. */
    public boolean isIntEverything() {
        return equalsRange(INT_EVERYTHING);
    }

    /** Return true if this range contains every {@code short} value. */
    public boolean isShortEverything() {
        return equalsRange(SHORT_EVERYTHING);
    }

    /** Return true if this range contains every {@code char} value. */
    public boolean isCharEverything() {
        return equalsRange(CHAR_EVERYTHING);
    }

    /** Return true if this range contains every {@code byte} value. */
    public boolean isByteEverything() {
        return equalsRange(BYTE_EVERYTHING);
    }

    /** Return true if this range contains no values. */
    public boolean isNothing() {
        return this == NOTHING;
    }

    /** The number of values representable in 32 bits: 2^32 or {@code 1<<32}. */
    private static final long INT_WIDTH = INT_EVERYTHING.width();

    /**
     * Converts this range to a 32-bit integral range.
     *
     * <p>If {@link #ignoreOverflow} is true and one of the bounds is outside the Integer range,
     * then that bound is set to the bound of the Integer range.
     *
     * <p>If {@link #ignoreOverflow} is false and this range is too wide, i.e., wider than the full
     * range of the Integer class, return INT_EVERYTHING.
     *
     * <p>If {@link #ignoreOverflow} is false and the bounds of this range are not representable as
     * 32-bit integers, convert the bounds to Integer type in accordance with Java twos-complement
     * overflow rules, e.g., Integer.MAX_VALUE + 1 is converted to Integer.MIN_VALUE.
     */
    public Range intRange() {
        if (this.isNothing()) {
            return this;
        }
        if (INT_EVERYTHING.contains(this)) {
            return this;
        }
        if (ignoreOverflow) {
            return create(clipToRange(from, INT_EVERYTHING), clipToRange(to, INT_EVERYTHING));
        }
        if (this.isWiderThan(INT_WIDTH)) {
            return INT_EVERYTHING;
        }
        return createOrElse((int) this.from, (int) this.to, INT_EVERYTHING);
    }

    /** The number of values representable in 16 bits: 2^16 or 1&lt;&lt;16. */
    private static final long SHORT_WIDTH = SHORT_EVERYTHING.width();

    /**
     * Converts a this range to a 16-bit short range.
     *
     * <p>If {@link #ignoreOverflow} is true and one of the bounds is outside the Short range, then
     * that bound is set to the bound of the Short range.
     *
     * <p>If {@link #ignoreOverflow} is false and this range is too wide, i.e., wider than the full
     * range of the Short class, return SHORT_EVERYTHING.
     *
     * <p>If {@link #ignoreOverflow} is false and the bounds of this range are not representable as
     * 16-bit integers, convert the bounds to Short type in accordance with Java twos-complement
     * overflow rules, e.g., Short.MAX_VALUE + 1 is converted to Short.MIN_VALUE.
     */
    public Range shortRange() {
        if (this.isNothing()) {
            return this;
        }
        if (SHORT_EVERYTHING.contains(this)) {
            return this;
        }
        if (ignoreOverflow) {
            return create(clipToRange(from, SHORT_EVERYTHING), clipToRange(to, SHORT_EVERYTHING));
        }
        if (this.isWiderThan(SHORT_WIDTH)) {
            // short is promoted to int before the operation so no need for explicit casting
            return SHORT_EVERYTHING;
        }
        return createOrElse((short) this.from, (short) this.to, SHORT_EVERYTHING);
    }

    /** The number of values representable in char: */
    private static final long CHAR_WIDTH = CHAR_EVERYTHING.width();

    /**
     * Converts this range to a char range.
     *
     * <p>If {@link #ignoreOverflow} is true and one of the bounds is outside the Character range,
     * then that bound is set to the bound of the Character range.
     *
     * <p>If {@link #ignoreOverflow} is false and this range is too wide, i.e., wider than the full
     * range of the Character class, return CHAR_EVERYTHING.
     *
     * <p>If {@link #ignoreOverflow} is false and the bounds of this range are not representable as
     * 8-bit integers, convert the bounds to Character type in accordance with Java overflow rules
     * (twos-complement), e.g., Character.MAX_VALUE + 1 is converted to Character.MIN_VALUE.
     */
    public Range charRange() {
        if (this.isNothing()) {
            return this;
        }
        if (CHAR_EVERYTHING.contains(this)) {
            return this;
        }
        if (ignoreOverflow) {
            return create(clipToRange(from, CHAR_EVERYTHING), clipToRange(to, CHAR_EVERYTHING));
        }
        if (this.isWiderThan(CHAR_WIDTH)) {
            // char is promoted to int before the operation so no need for explicit casting
            return CHAR_EVERYTHING;
        }
        return createOrElse((char) this.from, (char) this.to, CHAR_EVERYTHING);
    }

    /** The number of values representable in 8 bits: 2^8 or 1&lt;&lt;8. */
    private static final long BYTE_WIDTH = BYTE_EVERYTHING.width();

    /**
     * Converts this range to a 8-bit byte range.
     *
     * <p>If {@link #ignoreOverflow} is true and one of the bounds is outside the Byte range, then
     * that bound is set to the bound of the Byte range.
     *
     * <p>If {@link #ignoreOverflow} is false and this range is too wide, i.e., wider than the full
     * range of the Byte class, return BYTE_EVERYTHING.
     *
     * <p>If {@link #ignoreOverflow} is false and the bounds of this range are not representable as
     * 8-bit integers, convert the bounds to Byte type in accordance with Java twos-complement
     * overflow rules, e.g., Byte.MAX_VALUE + 1 is converted to Byte.MIN_VALUE.
     */
    public Range byteRange() {
        if (this.isNothing()) {
            return this;
        }
        if (BYTE_EVERYTHING.contains(this)) {
            return this;
        }
        if (ignoreOverflow) {
            return create(clipToRange(from, BYTE_EVERYTHING), clipToRange(to, BYTE_EVERYTHING));
        }
        if (this.isWiderThan(BYTE_WIDTH)) {
            // byte is promoted to int before the operation so no need for explicit casting
            return BYTE_EVERYTHING;
        }
        return createOrElse((byte) this.from, (byte) this.to, BYTE_EVERYTHING);
    }

    /**
     * Return x clipped to the given range; out-of-range values become extremal values. Appropriate
     * only when {@link #ignoreOverflow} is true.
     *
     * @param x a value
     * @param r a range
     * @return a value within the range; if x is outside r, returns the min or max of r
     */
    private long clipToRange(long x, Range r) {
        if (x < r.from) {
            return r.from;
        } else if (x > r.to) {
            return r.to;
        } else {
            return x;
        }
    }

    /**
     * Returns true if the element is contained in this range.
     *
     * @param element the value to seek
     * @return true if {@code element} is in this range
     */
    public boolean contains(long element) {
        return from <= element && element <= to;
    }

    /**
     * Returns true if the other range is contained in this range.
     *
     * @param other the range that might be within this one
     * @return true if {@code other} is within this range
     */
    public boolean contains(Range other) {
        return other.isWithin(from, to);
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
        return create(resultFrom, resultTo);
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
        return createOrNothing(resultFrom, resultTo);
    }

    /**
     * Returns the range with the lowest to and from values of this range and the passed range.
     *
     * @param other the range to compare
     * @return the range with the lowest to and from values of this range and the passed range
     */
    public Range min(Range other) {
        return create(Math.min(this.from, other.from), Math.min(this.to, other.to));
    }

    /**
     * Returns the range with the highest to and from values of this range and the passed range.
     *
     * @param other the range to compare
     * @return the range with the highest to and from values of this range and the passed range
     */
    public Range max(Range other) {
        return create(Math.max(this.from, other.from), Math.max(this.to, other.to));
    }

    /**
     * Returns the smallest range that includes all possible values resulting from adding an
     * arbitrary value in the specified range to an arbitrary value in this range. We call this the
     * addition of two ranges.
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
            if (from > to) {
                return Range.EVERYTHING;
            } else {
                return create(resultFrom, resultTo);
            }
        } else {
            BigInteger bigFrom = BigInteger.valueOf(from).add(BigInteger.valueOf(right.from));
            BigInteger bigTo = BigInteger.valueOf(to).add(BigInteger.valueOf(right.to));
            return create(bigFrom, bigTo);
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
            return create(resultFrom, resultTo);
        } else {
            BigInteger bigFrom = BigInteger.valueOf(from).subtract(BigInteger.valueOf(right.to));
            BigInteger bigTo = BigInteger.valueOf(to).subtract(BigInteger.valueOf(right.from));
            return create(bigFrom, bigTo);
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
            return create(possibleValues);
        } else {
            final BigInteger bigLeftFrom = BigInteger.valueOf(from);
            final BigInteger bigRightFrom = BigInteger.valueOf(right.from);
            final BigInteger bigRightTo = BigInteger.valueOf(right.to);
            final BigInteger bigLeftTo = BigInteger.valueOf(to);
            List<BigInteger> bigPossibleValues =
                    Arrays.asList(
                            bigLeftFrom.multiply(bigRightFrom),
                            bigLeftFrom.multiply(bigRightTo),
                            bigLeftTo.multiply(bigRightFrom),
                            bigLeftTo.multiply(bigRightTo));
            return create(Collections.min(bigPossibleValues), Collections.max(bigPossibleValues));
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
                return create(Long.MIN_VALUE, Long.MIN_VALUE / -2);
            } else {
                // Special case 3:
                // This range contains only Long.MIN_VALUE, and right contains only -1.
                return create(Long.MIN_VALUE, Long.MIN_VALUE);
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
        return create(resultFrom, resultTo);
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
                    range = create(0, 0);
                } else { // (to > Long.MIN_VALUE)
                    // When this range contains Long.MIN_VALUE, which would have a remainder of 0 if
                    // divided by Long.MIN_VALUE, the result range is {0} unioned with [from + 1,
                    // to]
                    range = create(from + 1, to).union(create(0, 0));
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
                Range rangeAdditional = this.remainder(create(right.from + 1, right.to));
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
                create(Math.max(Long.MIN_VALUE + 1, from), Math.max(Long.MIN_VALUE + 1, to))
                        .union(create(0, 0));
        // Calculate range2: the result range of range EVERYTHING divided by the right range. For
        // example, if the right range is [-5, 3], then the result range would be [-4, 4]. If the
        // right range is [3, 6], then the result range would be [-5, 5]. In general, the result
        // range is calculated as following:
        long maxAbsolute = Math.max(Math.abs(right.from), Math.abs(right.to));
        Range range2 = create(-maxAbsolute + 1, maxAbsolute - 1);
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
        // If the left-hand operand is int  type, only the 5 lowest-order bits of the right-hand
        // operand are used.
        // If the left-hand operand is long type, only the 6 lowest-order bits of the right-hand
        // operand are used.
        // For example, while 1 << -1== 1 << 31, 1L << -1 == 1L << 63.
        // For ths reason, we restrict the shift-bits to analyze in [0. 31] and give up the analysis
        // when out of this range.
        //
        // Other possible solutions:
        // 1. create different methods for int type and long type and use them accordingly
        // 2. add an additional boolean parameter to indicate the type of the left-hand operand
        //
        // see https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.19 for more
        // detail.
        if (right.isWithin(0, 31)) {
            if (this.isWithinInteger()) {
                // This bound is adequate to guarantee no overflow when using long to evaluate
                long resultFrom = from << (from >= 0 ? right.from : right.to);
                long resultTo = to << (to >= 0 ? right.to : right.from);
                return create(resultFrom, resultTo);
            } else {
                BigInteger bigFrom =
                        BigInteger.valueOf(from)
                                .shiftLeft(from >= 0 ? (int) right.from : (int) right.to);
                BigInteger bigTo =
                        BigInteger.valueOf(to)
                                .shiftLeft(to >= 0 ? (int) right.to : (int) right.from);
                return create(bigFrom, bigTo);
            }
        } else {
            // In other cases, we give up on the calculation and return EVERYTHING (rare in
            // practice).
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
            return create(resultFrom, resultTo);
        } else {
            // Signed shift right operation for long type cannot be simulated with BigInteger.
            // Give up on the calculation and return EVERYTHING instead.
            return EVERYTHING;
        }
    }

    /**
     * When this range only contains non-negative values, the refined result should be the same as
     * {@link #signedShiftRight(Range)}. We give up the analysis when this range contains negative
     * value(s).
     */
    public Range unsignedShiftRight(Range right) {
        if (this.from >= 0) {
            return signedShiftRight(right);
        }

        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        return EVERYTHING;
    }

    /**
     * Returns a range that includes all possible values resulting from performing the bitwise and
     * operation on a value in this range by a mask in the specified range. We call this the bitwise
     * and operation of a range.
     *
     * <p>The current implementation is conservative: it only refines the cases where the range of
     * mask represents a constant. In other cases, it gives up on the refinement and returns {@code
     * EVERYTHING} instead.
     *
     * @param right the range of mask of the bitwise and operation
     * @return the range resulting from the bitwise and operation of this range and the specified
     *     range of mask
     */
    public Range bitwiseAnd(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        // We only refine the cases where the range of mask represent a constant.
        // Recall these two's-complement facts:
        //   11111111  represents  -1
        //   10000000  represents  MIN_VALUE

        Range constant = null;
        Range variable = null;
        if (right.isConstant()) {
            constant = right;
            variable = this;
        } else if (this.isConstant()) {
            constant = this;
            variable = right;
        }

        if (constant != null) {
            long mask = constant.from;
            if (mask >= 0) {
                // Sign bit of mask is 0.  The elements in the result range must be positive, and
                // the result range is upper-bounded by the mask.
                if (variable.from >= 0) {
                    // Case 1.1: The result range is upper-bounded by the upper bound of this range.
                    return create(0, Math.min(mask, variable.to));
                } else if (variable.to < 0) {
                    // Case 1.2: The result range is upper-bounded by the upper bound of this range
                    // after ignoring the sign bit. The upper bound of this range has the most bits
                    // (of the highest place values) set to 1.
                    return create(0, Math.min(mask, noSignBit(variable.to)));
                } else {
                    // Case 1.3:  Since this range contains -1, the upper bound of this range after
                    // ignoring the sign bit is Long.MAX_VALUE and thus doesn't contribute to
                    // further refinement.
                    return create(0, mask);
                }
            } else {
                // Sign bit of mask is 1.
                if (variable.from >= 0) {
                    // Case 2.1: Similar to case 1.1 except that the sign bit of the mask can be
                    // ignored.
                    return create(0, Math.min(noSignBit(mask), variable.to));
                } else if (variable.to < 0) {
                    // Case 2.2: The sign bit of the elements in the result range must be 1.
                    // Therefore the lower bound of the result range is Long.MIN_VALUE (when all
                    // 1-bits are mismatched between the mask and the element in this range). The
                    // result range is also upper-bounded by this mask itself and the upper bound of
                    // this range.  (Because more set bits means a larger number -- still negative,
                    // but closer to 0.)
                    return create(Long.MIN_VALUE, Math.min(mask, variable.to));
                } else {
                    // Case 2.3: Similar to case 2.2 except that the elements in this range could
                    // be positive, and thus the result range is upper-bounded by the upper bound
                    // of this range and the mask after ignoring the sign bit.
                    return create(Long.MIN_VALUE, Math.min(noSignBit(mask), variable.to));
                }
            }
        }

        return EVERYTHING;
    }

    /** Return the argument, with its sign bit zeroed out. */
    private long noSignBit(Long mask) {
        return mask & (-1L >>> 1);
    }

    /** We give up the analysis for bitwise OR operation. */
    public Range bitwiseOr(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

        return EVERYTHING;
    }

    /** We give up the analysis for bitwise XOR operation. */
    public Range bitwiseXor(Range right) {
        if (this.isNothing() || right.isNothing()) {
            return NOTHING;
        }

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

        return create(-to, -from);
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

        return create(~to, ~from);
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
        return createOrNothing(from, resultTo);
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
        return createOrNothing(from, resultTo);
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
        return createOrNothing(resultFrom, to);
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
        return createOrNothing(resultFrom, to);
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
        if (right.isConstant()) {
            if (this.to == right.to) {
                return create(this.from, this.to - 1);
            } else if (this.from == right.from) {
                return create(this.from + 1, this.to);
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
            return width() > value;
        } else {
            return BigInteger.valueOf(to)
                            .subtract(BigInteger.valueOf(from))
                            .add(BigInteger.ONE)
                            .compareTo(BigInteger.valueOf(value))
                    > 0;
        }
    }

    /** Determines if this range represents a constant value. */
    public boolean isConstant() {
        return from == to;
    }

    /**
     * Determines if this range is completely contained in the range specified by the given lower
     * bound inclusive and upper bound inclusive.
     *
     * @param lb lower bound for the range that might contain this one
     * @param ub upper bound for the range that might contain this one
     * @return true if this range is within the given bounds
     */
    public boolean isWithin(long lb, long ub) {
        assert lb <= ub;
        return lb <= from && to <= ub;
    }

    /**
     * Determines if this range is contained inclusively between Long.MIN_VALUE/2 and
     * Long.MAX_VALUE/2. Note: Long.MIN_VALUE/2 != -Long.MAX_VALUE/2
     */
    private boolean isWithinHalfLong() {
        return isWithin(Long.MIN_VALUE >> 1, Long.MAX_VALUE >> 1);
    }

    /**
     * Determines if this range is completely contained in the scope of the Integer type.
     *
     * @return true if the range is contained within the Integer range inclusive
     */
    public boolean isWithinInteger() {
        return isWithin(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
