package org.checkerframework.common.value.util;

import java.math.BigInteger;
/**
 * The Range Class with mathematics operations
 *
 * @author JasonMrX
 */
public class Range {

    public final long from;
    public final long to;

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

    private Range getRangeFromPossibleValues(long[] possibleValues) {
        long resultFrom = Long.MAX_VALUE;
        long resultTo = Long.MIN_VALUE;
        for (long pv : possibleValues) {
            resultFrom = Math.min(resultFrom, pv);
            resultTo = Math.max(resultTo, pv);
        }
        return new Range(resultFrom, resultTo);
    }

    public Range union(Range right) {
        long resultFrom = Math.min(from, right.from);
        long resultTo = Math.max(to, right.to);
        return new Range(resultFrom, resultTo);
    }

    public Range intersect(Range right) {
        long resultFrom = Math.max(from, right.from);
        long resultTo = Math.min(to, right.to);
        return new Range(resultFrom, resultTo);
    }

    public Range plus(Range right) {
        long resultFrom = from + right.from;
        long resultTo = to + right.to;
        return new Range(resultFrom, resultTo);
    }

    public Range minus(Range right) {
        long resultFrom = from - right.to;
        long resultTo = to - right.from;
        return new Range(resultFrom, resultTo);
    }

    public Range times(Range right) {
        long[] possibleValues = new long[4];
        possibleValues[0] = from * right.from;
        possibleValues[1] = from * right.to;
        possibleValues[2] = to * right.from;
        possibleValues[3] = to * right.to;
        return getRangeFromPossibleValues(possibleValues);
    }

    public Range divide(Range right) {
        long resultFrom = Long.MIN_VALUE;
        long resultTo = Long.MAX_VALUE;

        // TODO: be careful of divided by zero!
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

    public Range remainder(Range right) {
        /*
         * calculate the bounds case by case:
         * + / + : [0, min(l, r - 1)]
         * + / - : [0, min(l, -r - 1)]
         * - / + : [max(l, -r + 1), 0]
         * - / - : [max(l, r + 1), 0]
         *
         * too many different conditions
         * return a looser range
         */
        long[] possibleValues = new long[9];
        possibleValues[0] = 0;
        possibleValues[1] = Math.min(from, Math.abs(right.from) - 1);
        possibleValues[2] = Math.min(from, Math.abs(right.to) - 1);
        possibleValues[3] = Math.min(to, Math.abs(right.from) - 1);
        possibleValues[4] = Math.min(to, Math.abs(right.to) - 1);
        possibleValues[5] = Math.max(from, -Math.abs(right.from) + 1);
        possibleValues[6] = Math.max(from, -Math.abs(right.to) + 1);
        possibleValues[7] = Math.max(to, -Math.abs(right.from) + 1);
        possibleValues[8] = Math.max(to, -Math.abs(right.to) + 1);
        return getRangeFromPossibleValues(possibleValues);
    }

    public Range shiftLeft(Range right) {
        // TODO: warning if right operand may be out of [0, 31]
        if (right.from < 0 || right.from > 31 || right.to < 0 || right.to > 31) {
            return new Range();
        }
        long resultFrom = from << (from >= 0 ? right.from : right.to);
        long resultTo = to << (to >= 0 ? right.to : right.from);
        return new Range(resultFrom, resultTo);
    }

    public Range signedShiftRight(Range right) {
        if (right.from < 0 || right.from > 31 || right.to < 0 || right.to > 31) {
            return new Range();
        }
        long resultFrom = from >> (from >= 0 ? right.to : right.from);
        long resultTo = to >> (to >= 0 ? right.from : right.to);
        return new Range(resultFrom, resultTo);
    }

    public Range unaryPlus() {
        return new Range(from, to);
    }

    public Range unaryMinus() {
        return new Range(-to, -from);
    }

    public Range bitwiseComplement() {
        return new Range(~to, ~from);
    }

    public Range lessThan(Range right) {
        return new Range(from, Math.min(to, right.to - 1));
    }

    public Range lessThanEq(Range right) {
        return new Range(from, Math.min(to, right.to));
    }

    public Range greaterThan(Range right) {
        return new Range(Math.max(from, right.from + 1), to);
    }

    public Range greaterThanEq(Range right) {
        return new Range(Math.max(from, right.from), to);
    }

    public Range equalTo(Range right) {
        return new Range(Math.max(from, right.from), Math.min(to, right.to));
    }

    public Range notEqualTo(Range right) {
        return new Range(from, to);
    }

    public BigInteger numberOfPossibleValues() {
        return BigInteger.valueOf(to).subtract(BigInteger.valueOf(from)).add(BigInteger.valueOf(1));
    }

    public boolean isWiderThan(int value) {
        return numberOfPossibleValues().compareTo(BigInteger.valueOf(value)) == 1;
    }
    /*
     * TODO:
     * How to handle overflow/underflow?
     * How to handle division by zero?
     * How to handle shift out of range [0, 31]?
     */

}
