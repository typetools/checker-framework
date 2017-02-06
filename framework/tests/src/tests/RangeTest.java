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

    int[] rangeBounds = {
        Integer.MIN_VALUE,
        Integer.MIN_VALUE + 1,
        -1000,
        -100,
        -10,
        -3,
        -2,
        -1,
        0,
        1,
        2,
        3,
        10,
        100,
        1000,
        Integer.MAX_VALUE - 1,
        Integer.MAX_VALUE
    };
    int[] values = {
        Integer.MIN_VALUE,
        Integer.MIN_VALUE + 1,
        -1000,
        -500,
        -100,
        -20,
        -10,
        -9,
        -8,
        -7,
        -6,
        -5,
        -4,
        -3,
        -2,
        -1,
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        20,
        100,
        500,
        1000,
        Integer.MAX_VALUE - 1,
        Integer.MAX_VALUE
    };

    Range[] ranges;

    public RangeTest() {
        // Initialize the ranges list.
        List<Range> rangesList = new ArrayList<Range>();
        for (int lowerbound : rangeBounds) {
            for (int upperbound : rangeBounds) {
                if (lowerbound < upperbound) {
                    rangesList.add(new Range(lowerbound, upperbound));
                }
            }
        }
        ranges = rangesList.toArray(new Range[0]);
    }

    /** The element is a member of the range. */
    class RangeAndElement {
        Range range;
        int element;

        RangeAndElement(Range range, int element) {
            if (!range.contains(element)) {
                throw new IllegalArgumentException();
            }
            this.range = range;
            this.element = element;
        }
    }

    class RangeAndTwoElements {
        Range range;
        int a;
        int b;
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

    class ValuesInRangeIterator implements Iterator<Integer>, Iterable<Integer> {

        Range range;
        // This is the first index that has NOT been examined.  It is in [0..values.length].
        int i = 0;
        int nextValue;
        boolean nextValueValid = false;

        public ValuesInRangeIterator(Range range) {
            this.range = range;
        }

        @Override
        public Integer next() {
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
        public Iterator<Integer> iterator() {
            return this;
        }
    }

    @Test
    public void testUnion() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (int value : values) {
                    Range result = range1.union(range2);
                    assert ((range1.contains(value) || range2.contains(value))
                                    == (result.contains(value)))
                            : String.format(
                                    "Range.union failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testIntersect() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                Range result = range1.intersect(range2);
                for (int value : values) {
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
                Range result = re1.range.divide(re2.range);
                assert result.contains(re1.element / re2.element)
                        : String.format(
                                "Range.divide failure: %s %s => %s; witnesses %s / %s => %s",
                                re1.range,
                                re2.range,
                                result,
                                re1.element,
                                re2.element,
                                re1.element / re2.element);
            }
        }
    }

    @Test
    public void testRemainder() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                Range result = re1.range.divide(re2.range);
                assert result.contains(re1.element % re2.element)
                        : String.format(
                                "Range.divide failure: %s %s => %s; witnesses %s % %s => %s",
                                re1.range,
                                re2.range,
                                result,
                                re1.element,
                                re2.element,
                                re1.element % re2.element);
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
                for (int value : values) {
                    Range result = range1.lessThan(range2);
                    assert (range1.contains(value) || !result.contains(value))
                            : String.format(
                                    "Range.lessThan failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testLessThanEq() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (int value : values) {
                    Range result = range1.lessThanEq(range2);
                    assert (range1.contains(value) || !result.contains(value))
                            : String.format(
                                    "Range.lessThanEq failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testGreaterThan() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (int value : values) {
                    Range result = range1.greaterThan(range2);
                    assert (range1.contains(value) || !result.contains(value))
                            : String.format(
                                    "Range.greaterThan failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testGreaterThanEq() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (int value : values) {
                    Range result = range1.greaterThanEq(range2);
                    assert (range1.contains(value) || !result.contains(value))
                            : String.format(
                                    "Range.greaterThanEq failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }

    @Test
    public void testEqualTo() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                for (int value : values) {
                    Range result = range1.equalTo(range2);
                    assert (range1.contains(value) || !result.contains(value))
                            : String.format(
                                    "Range.equalTo failure: %s %s %s; witness = %s",
                                    range1, range2, result, value);
                }
            }
        }
    }
}
