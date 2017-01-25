package org.checkerframework.common.value;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * The Range Class with mathematics operations. Models the range indicated by the @IntRange
 * annotation
 *
 * <p>Note that this class is not responsible for detecting incorrect parameters, e.g. the value of
 * "from" could be greater than the value of "to". This incorrectness would be eventually caught by
 * {@link
 * org.checkerframework.common.value.ValueAnnotatedTypeFactory#createIntRangeAnnotation(Range)} when
 * creating an annotation from range, which would then be replaced with {@code
 * {@literal @}UnknownVal}
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
     * Returns the smallest single range that includes all values enclosed by either of the two
     * ranges. We call this the union of two ranges.
     *
     * <p>Special case:
     *
     * <ul>
     *   <li>If there is no overlap between two ranges, the gap between the two would be filled and
     *       thus still results in only one single range.
     * </ul>
     *
     * @param right a range to union with this range
     * @return a range resulted from the union of the specified range and this range.
     */
    public Range union(Range right) {
        long resultFrom = Math.min(from, right.from);
        long resultTo = Math.max(to, right.to);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns the smallest single range that includes all values enclosed by both of the two
     * ranges. We call this the intersection of two ranges.
     *
     * <p>Special case:
     *
     * <ul>
     *   <li>If there is no overlap between two ranges, an incorrect range with from greater than to
     *       would be returned. This incorrectness would be then caught by {@link
     *       org.checkerframework.common.value.ValueAnnotatedTypeFactory#createIntRangeAnnotation(Range)}
     *       when creating an annotation from range, which would then be replaced with
     *       {@code @UnknownVal}
     * </ul>
     *
     * @param right the range to intersect with this range
     * @return a range resulted from the intersection of the specified range and this range.
     */
    public Range intersect(Range right) {
        long resultFrom = Math.max(from, right.from);
        long resultTo = Math.min(to, right.to);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns the smallest single range that includes all possible values resulted from adding any
     * two values selected from two ranges respectively. We call this the addition of two ranges.
     *
     * @param right a range to be added to this range.
     * @return the range resulted from the addition of the specified range and this range.
     */
    public Range plus(Range right) {
        long resultFrom = from + right.from;
        long resultTo = to + right.to;
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns the smallest single range that includes all possible values resulted from subtracting
     * an arbitrary value in the specified range from an arbitrary value in this range. We call this
     * the subtraction of two ranges
     *
     * @param right the range to be subtracted from this range
     * @return the range resulted from subtracting the specified range from this range
     */
    public Range minus(Range right) {
        long resultFrom = from - right.to;
        long resultTo = to - right.from;
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns the smallest single range that includes all possible values resulted from multiplying
     * an arbitrary value in the specified range by an arbitrary value in this range. We call this
     * the multiplication of two ranges
     *
     * @param right the specified range to be multiplied by this range
     * @return the range resulted from multiplying the specified range by this range
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
     * Returns the smallest single range that includes all possible values resulted from dividing an
     * arbitrary value in this range by an arbitrary value in the specified range. We call this the
     * division of two ranges
     *
     * @param right the specified range by which this range is divided.
     * @return the range resulted from dividing this range by the specified range.
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
     * Returns the a single range that includes all possible values of the remainder of dividing an
     * arbitrary value in this range by an arbitrary value in the specified range.
     *
     * @param right the specified range by which this range is divided.
     * @return the range of the remainder of dividing this range by the specified range. Note that
     *     this range might not be the smallest single range that includes all the possible values.
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
     * Returns the smallest single range that includes all possible values resulted from left
     * shifting an arbitrary value in this range by an arbitrary number of bits in the specified
     * range. We call this the left shift operation of a range
     *
     * @param right the range of bits by which this range is left shifted.
     * @return the range resulted from left shifting this range by the specified range.
     */
    public Range shiftLeft(Range right) {
        long resultFrom = from << (from >= 0 ? right.from : right.to);
        long resultTo = to << (to >= 0 ? right.to : right.from);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns the smallest single range that includes all possible values resulted from signed
     * right shifting an arbitrary value in this range by an arbitrary number of bits in the
     * specified range. We call this the signed right shift operation of a range
     *
     * @param right the range of bits by which this range is signed right shifted.
     * @return the range resulted from signed right shifting this range by the specified range.
     */
    public Range signedShiftRight(Range right) {
        long resultFrom = from >> (from >= 0 ? right.to : right.from);
        long resultTo = to >> (to >= 0 ? right.from : right.to);
        return new Range(resultFrom, resultTo);
    }

    /**
     * Returns the range of a variable that falls within this range after applying unary plus
     * operation.
     *
     * @return the resulted range of applying unary plus on an arbitrary value in this range
     */
    public Range unaryPlus() {
        return new Range(from, to);
    }

    /**
     * Returns the range of a variable that falls within this range after applying unary minus
     * operation.
     *
     * @return the resulted range of applying unary minus on an arbitrary value in this range
     */
    public Range unaryMinus() {
        return new Range(-to, -from);
    }

    /**
     * Returns the range of a variable that falls within this range after applying bitwise
     * complement operation.
     *
     * @return the resulted range of applying bitwise complement on an arbitrary value in this range
     */
    public Range bitwiseComplement() {
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
     * void foo(
     *     {@literal @}IntRange(from = 0, to = 10) int a, // range of <i>a</i> is [0, 10]
     *     {@literal @}IntRange(from = 3, to = 5) int b) // range of <i>b</i> is [3, 5]
     *     if (a &lt; b) {
     *         // range of <i>a</i> is now refined to [0, 4] because a value in range [5, 10]
     *         // cannot be smaller than variable <i>b</i> with range [3, 5]
     *         ...
     *     }
     * }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range lessThan(Range right) {
        return new Range(from, Math.min(to, right.to - 1));
    }

    /**
     * Determines a refined range of a variable that fall within this {@code Range} under the
     * condition that this variable is less than or equal to another variable that fall within the
     * specified {@code Range}. This is used for calculating the control flow refined result of the
     * &lt;= operator. i.e.
     *
     * <pre>
     * <code>
     * void foo(
     *     {@literal @}IntRange(from = 0, to = 10) int a, // range of <i>a</i> is [0, 10]
     *     {@literal @}IntRange(from = 3, to = 5) int b) // range of <i>b</i> is [3, 5]
     *     if (a &lt;= b) {
     *         // range of <i>a</i> is now refined to [0, 5] because a value in range [6, 10]
     *         // cannot be less than or equal to variable <i>b</i> with range [3, 5]
     *         ...
     *     }
     * }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range lessThanEq(Range right) {
        return new Range(from, Math.min(to, right.to));
    }

    /**
     * Determines a refined range of a variable that fall within this {@code Range} under the
     * condition that this variable is greater than another variable that fall within the specified
     * {@code Range}. This is used for calculating the control flow refined result of the &gt;
     * operator. i.e.
     *
     * <pre>
     * <code>
     * void foo(
     *     {@literal @}IntRange(from = 0, to = 10) int a, // range of <i>a</i> is [0, 10]
     *     {@literal @}IntRange(from = 3, to = 5) int b) // range of <i>b</i> is [3, 5]
     *     if (a &gt; b) {
     *         // range of <i>a</i> is now refined to [6, 10] because a value in range [0, 5]
     *         // cannot be greater than variable <i>b</i> with range [3, 5]
     *         ...
     *     }
     * }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range greaterThan(Range right) {
        return new Range(Math.max(from, right.from + 1), to);
    }

    /**
     * Determines a refined range of a variable that fall within this {@code Range} under the
     * condition that this variable is greater than or equal to another variable that fall within
     * the specified {@code Range}. This is used for calculating the control flow refined result of
     * the &gt;= operator. i.e.
     *
     * <pre>
     * <code>
     * void foo(
     *     {@literal @}IntRange(from = 0, to = 10) int a, // range of <i>a</i> is [0, 10]
     *     {@literal @}IntRange(from = 3, to = 5) int b) // range of <i>b</i> is [3, 5]
     *     if (a &gt;= b) {
     *         // range of <i>a</i> is now refined to [5, 10] because a value in range [0, 4]
     *         // cannot be greater than or equal to variable <i>b</i> with range [3, 5]
     *         ...
     *     }
     * }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range greaterThanEq(Range right) {
        return new Range(Math.max(from, right.from), to);
    }

    /**
     * Determines a refined range of a variable that fall within this {@code Range} under the
     * condition that this variable is equal to another variable that fall within the specified
     * {@code Range}. This is used for calculating the control flow refined result of the ==
     * operator. i.e.
     *
     * <pre>
     * <code>
     * void foo(
     *     {@literal @}IntRange(from = 0, to = 10) int a, // range of <i>a</i> is [0, 10]
     *     {@literal @}IntRange(from = 3, to = 15) int b) // range of <i>b</i> is [3, 15]
     *     if (a == b) {
     *         // range of <i>a</i> is now refined to [3. 10] because a value in range [0, 2]
     *         // cannot be equal to variable <i>b</i> with range [3, 15]
     *         ...
     *     }
     * }
     * </code>
     * </pre>
     *
     * @param right the specified {@code Range} to compare with
     * @return the refined {@code Range}
     */
    public Range equalTo(Range right) {
        return new Range(Math.max(from, right.from), Math.min(to, right.to));
    }

    /**
     * <<<<<<< HEAD Control flow refinement for not equal to operator
     *
     * @param right the range to compare with
     * @return the refined result
     */
    public Range notEqualTo(Range right) {
        return new Range(from, to);
    }

    /**
     * Gets the number of possible values enclosed by this range. To prevent overflow, we use
     * BigInteger for calculation. ||||||| merged common ancestors Control flow refinement for not
     * equal to operator
     *
     * @param right the range to compare with
     * @return the refined result
     */
    public Range notEqualTo(Range right) {
        return new Range(from, to);
    }

    /**
     * Gets the number of possible values within this range. To prevent overflow, we use BigInteger
     * for calculation. ======= Returns the number of possible values enclosed by this range. To
     * prevent overflow, we use BigInteger for calculation and return a BitInteger. >>>>>>>
     * 269094d43386325239a73714a1d69ff48576eb4a
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
     * <p><<<<<<< HEAD
     *
     * @param value
     * @return true if wider than the given value. ||||||| merged common ancestors
     * @param value
     * @return true if wider than the given value =======
     * @param value the value to compare with.
     * @return true if wider than the given value. >>>>>>> 269094d43386325239a73714a1d69ff48576eb4a
     */
    public boolean isWiderThan(int value) {
        return numberOfPossibleValues().compareTo(BigInteger.valueOf(value)) == 1;
    }
}
