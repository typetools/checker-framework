package org.checkerframework.common.value;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * The Range Class with mathematics operations. Models the range indicated by the @IntRange
 * annotation
 *
 * @author JasonMrX
 */
public class Range {

    /** The value 'from' */
    public final long from;

    /** The value 'to' */
    public final long to;

    /**
     * Constructs a range with its bounds specified by two parameters, "from" and "to".
     *
     * <p>Note that it is possible to construct a range with incorrect parameters, e.g. the value of
     * "from" could be greater than the value to "to". This incorrectness would be caught by {@link
     * org.checkerframework.common.value.ValueAnnotatedTypeFactory#createIntRangeAnnotation(Range)}
     * when creating an annotation from range, which would then be replaced with @UnknownVal
     *
     * @param from the lower bound (inclusive)
     * @param to the higher bound (inclusive)
     */
    public Range(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public Range(long value) {
        this(value, value);
    }

    public Range() {
        this.from = Long.MIN_VALUE;
        this.to = Long.MAX_VALUE;
    }

    /**
     * Unions two ranges into one. If there is no overlap between two ranges, the gap between the
     * two would be filled and thus results in only one single range.
     *
     * @param right the range to union with this range
     * @return a range from the lowest possible value of the two ranges to the highest possible
     *     value of the two ranges
     */
    public Range union(Range right) {
        long resultFrom = Math.min(from, right.from);
        long resultTo = Math.max(to, right.to);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Intersects two ranges. If there is no overlap between two ranges, an incorrect range with
     * from greater than to would be returned. This incorrectness would be caught by {@link
     * org.checkerframework.common.value.ValueAnnotatedTypeFactory#createIntRangeAnnotation(Range)}
     * when creating an annotation from range, which would then be replaced with @UnknownVal
     *
     * @param right the range to intersect with this range
     * @return a range from
     */
    public Range intersect(Range right) {
        long resultFrom = Math.max(from, right.from);
        long resultTo = Math.min(to, right.to);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Adds one range to another.
     *
     * @param right the range to be added to this range
     * @return the smallest single range that includes all possible values resulted from adding any
     *     two values selected from two ranges respectively
     */
    public Range plus(Range right) {
        long resultFrom = from + right.from;
        long resultTo = to + right.to;
        return new Range(resultFrom, resultTo);
    }

    /**
     * Subtracts one range from another
     *
     * @param right the range to be subtracted from this range
     * @return the smallest single range that includes all possible values resulted from subtracting
     *     an arbitrary value in @param from an arbitrary value in this range
     */
    public Range minus(Range right) {
        long resultFrom = from - right.to;
        long resultTo = to - right.from;
        return new Range(resultFrom, resultTo);
    }

    /**
     * Multiplies one range by another
     *
     * @param right the range to be multiplied by this range
     * @return the smallest single range that includes all possible values resulted from multiply an
     *     arbitrary value in @param by an arbitrary value in this range
     */
    public Range times(Range right) {
        ArrayList<Long> possibleValues =
                new ArrayList<>(
                        Arrays.asList(
                                from * right.from,
                                from * right.to,
                                to * right.from,
                                to * right.to));
        return new Range(Collections.min(possibleValues), Collections.max(possibleValues));
    }

    /**
     * Divides one range by another
     *
     * @param right the range to divide this range
     * @return the smallest range that includes all possible values resulted from dividing an
     *     arbitrary value in this range by an arbitrary value in @param
     */
    public Range divide(Range right) {
        long resultFrom = Long.MIN_VALUE;
        long resultTo = Long.MAX_VALUE;

        // Here we assume devide-by-zero is checked and avoided.
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
     * Modulos one range by another
     *
     * @param right the range to divide this range
     * @return a range (not the smallest one) that include all possible values resulted from taking
     *     the remainder from dividing an arbitrary value in this range by an arbitrary value
     *     in @param
     */
    public Range remainder(Range right) {
        ArrayList<Long> possibleValues =
                new ArrayList<>(
                        Arrays.asList(
                                0L,
                                Math.min(from, Math.abs(right.from) - 1),
                                Math.min(from, Math.abs(right.to) - 1),
                                Math.min(to, Math.abs(right.from) - 1),
                                Math.min(to, Math.abs(right.to) - 1),
                                Math.max(from, -Math.abs(right.from) + 1),
                                Math.max(from, -Math.abs(right.to) + 1),
                                Math.max(to, -Math.abs(right.from) + 1),
                                Math.max(to, -Math.abs(right.to) + 1)));
        return new Range(Collections.min(possibleValues), Collections.max(possibleValues));
    }

    /**
     * Left shifts a range by another
     *
     * @param right the range of bits to be shifted
     * @return the range of the resulted value from left shifting an arbitrary value in this range
     *     by an arbitrary value in @param.
     */
    public Range shiftLeft(Range right) {
        // TODO: warning if right operand may be out of [0, 31]
        if (right.from < 0 || right.from > 31 || right.to < 0 || right.to > 31) {
            return new Range();
        }
        long resultFrom = from << (from >= 0 ? right.from : right.to);
        long resultTo = to << (to >= 0 ? right.to : right.from);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Signed right shifts a range by another
     *
     * @param right the range of bits to be shifted
     * @return the range of the resulted value from signed right shifting an arbitrary value in this
     *     range by an arbitrary value in @param.
     */
    public Range signedShiftRight(Range right) {
        if (right.from < 0 || right.from > 31 || right.to < 0 || right.to > 31) {
            return new Range();
        }
        long resultFrom = from >> (from >= 0 ? right.to : right.from);
        long resultTo = to >> (to >= 0 ? right.from : right.to);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Unary plus this range
     *
     * @return the resulted range of applying unary plus on an arbitrary value in this range
     */
    public Range unaryPlus() {
        return new Range(from, to);
    }

    /**
     * Unary minus this range
     *
     * @return the resulted range of applying unary minus on an arbitrary value in this range
     */
    public Range unaryMinus() {
        return new Range(-to, -from);
    }

    /**
     * Bitwise complements this range
     *
     * @return the resulted range of applying bitwise complement on an arbitrary value in this range
     */
    public Range bitwiseComplement() {
        return new Range(~to, ~from);
    }

    /**
     * Control flow refinement for less than operator
     *
     * @param right the range to compare with
     * @return the refined result
     */
    public Range lessThan(Range right) {
        return new Range(from, Math.min(to, right.to - 1));
    }

    /**
     * Control flow refinement for less than or equal to operator
     *
     * @param right the range to compare with
     * @return the refined result
     */
    public Range lessThanEq(Range right) {
        return new Range(from, Math.min(to, right.to));
    }

    /**
     * Control flow refinement for greater than operator
     *
     * @param right the range to compare with
     * @return the refined result
     */
    public Range greaterThan(Range right) {
        return new Range(Math.max(from, right.from + 1), to);
    }

    /**
     * Control flow refinement for greater than or equal to operator
     *
     * @param right the range to compare with
     * @return the refined result
     */
    public Range greaterThanEq(Range right) {
        return new Range(Math.max(from, right.from), to);
    }

    /**
     * Control flow refinement for equal to operator
     *
     * @param right the range to compare with
     * @return the refined result
     */
    public Range equalTo(Range right) {
        return new Range(Math.max(from, right.from), Math.min(to, right.to));
    }

    /**
     * Control flow refinement for not equal to operator
     *
     * @param right the range to compare with
     * @return the refined result
     */
    public Range notEqualTo(Range right) {
        return new Range(from, to);
    }

    /**
     * Gets the number of possible values enclosed by this range. To prevent overflow, we use
     * BigInteger for calculation.
     *
     * @return the number of possible values enclosed by this range.
     */
    public BigInteger numberOfPossibleValues() {
        return BigInteger.valueOf(to).subtract(BigInteger.valueOf(from)).add(BigInteger.valueOf(1));
    }

    /**
     * Determines if the range is wider than a given value, i.e., if the number of possible values
     * enclosed by this range is more than the given value.
     *
     * @param value
     * @return true if wider than the given value.
     */
    public boolean isWiderThan(int value) {
        return numberOfPossibleValues().compareTo(BigInteger.valueOf(value)) == 1;
    }
}
