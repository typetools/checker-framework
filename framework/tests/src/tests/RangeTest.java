package tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.checkerframework.common.value.Range;
import org.junit.Test;

/** This class tests the Range class, independent of the Value Checker. */
public class RangeTest {

    // These sets of values may be excessively long; perhaps trim them down later.

    long[] rangeBounds = {
        Long.MIN_VALUE,
        Long.MIN_VALUE + 1,
        Integer.MIN_VALUE,
        Integer.MAX_VALUE + 1,
        -1000L,
        -100L,
        -10L,
        -3L,
        -2L,
        -1L,
        0L,
        1L,
        2L,
        3L,
        10L,
        100L,
        1000L,
        Integer.MAX_VALUE - 1,
        Integer.MAX_VALUE,
        Long.MAX_VALUE - 1,
        Long.MAX_VALUE
    };
    long[] values = {
        Long.MIN_VALUE,
        Long.MIN_VALUE + 1,
        Integer.MIN_VALUE,
        Integer.MIN_VALUE + 1,
        -1000L,
        -500L,
        -100L,
        -20L,
        -10L,
        -9L,
        -8L,
        -7L,
        -6L,
        -5L,
        -4L,
        -3L,
        -2L,
        -1L,
        0L,
        1L,
        2L,
        3L,
        4L,
        5L,
        6L,
        7L,
        8L,
        9L,
        10L,
        20L,
        100L,
        500L,
        1000L,
        Integer.MAX_VALUE - 1,
        Integer.MAX_VALUE,
        Long.MAX_VALUE - 1,
        Long.MAX_VALUE
    };

    Range[] ranges;

    public RangeTest() {
        // Initialize the ranges list.
        List<Range> rangesList = new ArrayList<Range>();
        for (long lowerbound : rangeBounds) {
            for (long upperbound : rangeBounds) {
                if (lowerbound <= upperbound) {
                    rangesList.add(new Range(lowerbound, upperbound));
                }
            }
        }
        ranges = rangesList.toArray(new Range[0]);
    }

    /** The element is a member of the range. */
    class RangeAndElement {
        Range range;
        long element;

        RangeAndElement(Range range, long element) {
            if (!range.contains(element)) {
                throw new IllegalArgumentException();
            }
            this.range = range;
            this.element = element;
        }
    }

    class RangeAndTwoElements {
        Range range;
        long a;
        long b;
    }

    ValuesInRangeIterator valuesInRange(Range r) {
        return new ValuesInRangeIterator(r);
    }

    RangeAndElementIterator rangeAndElements() {
        return new RangeAndElementIterator();
    }

    class RangeAndElementIterator implements Iterator<RangeAndElement>, Iterable<RangeAndElement> {
        // This is the index of the range that is currently being examined.
        // It is in [0..ranges.length].
        int ri;
        Range range;
        ValuesInRangeIterator vi;
        RangeAndElement nextValue;
        boolean nextValueValid = false;

        public RangeAndElementIterator() {
            ri = 0;
            range = ranges[ri];
            vi = new ValuesInRangeIterator(range);
        }

        @Override
        public RangeAndElement next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            nextValueValid = false;
            return nextValue;
        }

        @Override
        public boolean hasNext() {
            if (nextValueValid) {
                return true;
            }
            while (!vi.hasNext()) {
                ri++;
                if (ri == ranges.length) {
                    return false;
                }
                range = ranges[ri];
                vi = new ValuesInRangeIterator(range);
            }
            nextValue = new RangeAndElement(range, vi.next());
            nextValueValid = true;
            return true;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<RangeAndElement> iterator() {
            return this;
        }
    }

    class ValuesInRangeIterator implements Iterator<Long>, Iterable<Long> {

        Range range;
        // This is the first index that has NOT been examined.  It is in [0..values.length].
        int i = 0;
        long nextValue;
        boolean nextValueValid = false;

        public ValuesInRangeIterator(Range range) {
            this.range = range;
        }

        @Override
        public Long next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            nextValueValid = false;
            return nextValue;
        }

        @Override
        public boolean hasNext() {
            if (nextValueValid) {
                return true;
            }
            while (i < values.length) {
                nextValue = values[i];
                i++;
                if (range.contains(nextValue)) {
                    nextValueValid = true;
                    break;
                }
            }
            return nextValueValid;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Long> iterator() {
            return this;
        }
    }

    @Test
    public void testUnion() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (long value : values) {
                    Range result = range1.union(range2);
                    if (range1.contains(value) || range2.contains(value)) {
                        assert result.contains(value)
                                : String.format(
                                        "Range.union failure: %s %s %s; witness = %s",
                                        range1, range2, result, value);
                    }
                }
            }
        }
    }

    @Test
    public void testIntersect() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                Range result = range1.intersect(range2);
                for (long value : values) {
                    assert ((range1.contains(value) && range2.contains(value))
                                    == (result.contains(value)))
                            : String.format(
                                    "Range.intersect failure: %s %s => %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testPlus() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                Range result = re1.range.plus(re2.range);
                assert result.contains(re1.element + re2.element)
                        : String.format(
                                "Range.plus failure: %s %s => %s; witnesses %s + %s => %s",
                                re1.range,
                                re2.range,
                                result,
                                re1.element,
                                re2.element,
                                re1.element + re2.element);
            }
        }
    }

    @Test
    public void testMinus() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                Range result = re1.range.minus(re2.range);
                assert result.contains(re1.element - re2.element)
                        : String.format(
                                "Range.minus failure: %s %s => %s; witnesses %s - %s => %s",
                                re1.range,
                                re2.range,
                                result,
                                re1.element,
                                re2.element,
                                re1.element - re2.element);
            }
        }
    }

    @Test
    public void testTimes() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                Range result = re1.range.times(re2.range);
                assert result.contains(re1.element * re2.element)
                        : String.format(
                                "Range.times failure: %s %s => %s; witnesses %s * %s => %s",
                                re1.range,
                                re2.range,
                                result,
                                re1.element,
                                re2.element,
                                re1.element * re2.element);
            }
        }
    }

    @Test
    public void testDivide() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                if (re2.element == 0) {
                    continue;
                }
                Range result = re1.range.divide(re2.range);
                Long witness = re1.element / re2.element;
                assert result.contains(witness)
                        : String.format(
                                "Range.divide failure: %s %s => %s; witnesses %s / %s => %s",
                                re1.range, re2.range, result, re1.element, re2.element, witness);
            }
        }
    }

    @Test
    public void testRemainder() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                if (re2.element == 0) {
                    continue;
                }
                Range result = re1.range.remainder(re2.range);
                Long witness = re1.element % re2.element;
                assert result.contains(witness)
                        : String.format(
                                "Range.divide failure: %s %s => %s; witnesses %s % %s => %s",
                                re1.range, re2.range, result, re1.element, re2.element, witness);
            }
        }
    }

    @Test
    public void testShiftLeft() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                Range result = re1.range.shiftLeft(re2.range);
                assert result.contains(re1.element << re2.element)
                        : String.format(
                                "Range.shiftLeft failure: %s %s => %s; witnesses %s << %s => %s",
                                re1.range,
                                re2.range,
                                result,
                                re1.element,
                                re2.element,
                                re1.element << re2.element);
            }
        }
    }

    @Test
    public void testSignedShiftRight() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                Range result = re1.range.signedShiftRight(re2.range);
                assert result.contains(re1.element >> re2.element)
                        : String.format(
                                "Range.signedShiftRight failure: %s %s => %s; witnesses %s >> %s => %s",
                                re1.range,
                                re2.range,
                                result,
                                re1.element,
                                re2.element,
                                re1.element >> re2.element);
            }
        }
    }

    @Test
    public void testUnaryPlus() {
        for (RangeAndElement re : rangeAndElements()) {
            Range result = re.range.unaryPlus();
            assert result.contains(+re.element)
                    : String.format(
                            "Range.unaryPlus failure: %s => %s; witness %s => %s",
                            re.range, result, re.element, +re.element);
        }
    }

    @Test
    public void testUnaryMinus() {
        for (RangeAndElement re : rangeAndElements()) {
            Range result = re.range.unaryMinus();
            assert result.contains(-re.element)
                    : String.format(
                            "Range.unaryMinus failure: %s => %s; witness %s => %s",
                            re.range, result, re.element, -re.element);
        }
    }

    @Test
    public void testBitwiseComplement() {
        for (RangeAndElement re : rangeAndElements()) {
            Range result = re.range.bitwiseComplement();
            assert result.contains(~re.element)
                    : String.format(
                            "Range.bitwiseComplement failure: %s => %s; witness %s => %s",
                            re.range, result, re.element, ~re.element);
        }
    }

    @Test
    public void testLessThan() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (long value : values) {
                    Range result = range1.refineLessThan(range2);
                    assert (value >= range2.to
                                    ? !result.contains(value)
                                    : range1.contains(value) == result.contains(value))
                            : String.format(
                                    "Range.refineLessThan failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testLessThanEq() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (long value : values) {
                    Range result = range1.refineLessThanEq(range2);
                    assert (value > range2.to
                                    ? !result.contains(value)
                                    : range1.contains(value) == result.contains(value))
                            : String.format(
                                    "Range.refineLessThanEq failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testGreaterThan() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (long value : values) {
                    Range result = range1.refineGreaterThan(range2);
                    assert (value <= range2.from
                                    ? !result.contains(value)
                                    : range1.contains(value) == result.contains(value))
                            : String.format(
                                    "Range.refineGreaterThan failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testGreaterThanEq() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (long value : values) {
                    Range result = range1.refineGreaterThanEq(range2);
                    assert (value < range2.from
                                    ? !result.contains(value)
                                    : range1.contains(value) == result.contains(value))
                            : String.format(
                                    "Range.refineGreaterThanEq failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testEqualTo() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (long value : values) {
                    Range result = range1.refineEqualTo(range2);
                    assert (value < range2.from || value > range2.to
                                    ? !result.contains(value)
                                    : range1.contains(value) == result.contains(value))
                            : String.format(
                                    "Range.refineEqualTo failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }
}
