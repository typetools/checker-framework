package org.checkerframework.framework.test.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.lang.model.type.TypeKind;
import org.checkerframework.common.value.util.Range;
import org.junit.Assert;
import org.junit.Test;

/** This class tests the Range class, independent of the Value Checker. */
public class RangeTest {

    // These sets of values may be excessively long; perhaps trim them down later.

    long[] rangeBounds = {
        Long.MIN_VALUE,
        Long.MIN_VALUE + 1,
        Integer.MIN_VALUE - 1000L,
        Integer.MIN_VALUE - 10L,
        Integer.MIN_VALUE,
        Integer.MIN_VALUE + 1L,
        Short.MIN_VALUE - 1000L,
        Short.MIN_VALUE - 10L,
        Short.MIN_VALUE,
        Short.MIN_VALUE + 1L,
        Character.MIN_VALUE - 1000L,
        Character.MIN_VALUE - 10L,
        Character.MIN_VALUE, // 0L
        Character.MIN_VALUE + 1L,
        Byte.MIN_VALUE - 1000L,
        Byte.MIN_VALUE - 10L,
        Byte.MIN_VALUE,
        Byte.MIN_VALUE + 1L,
        Byte.MAX_VALUE - 1L,
        Byte.MAX_VALUE,
        Byte.MAX_VALUE + 10L,
        Byte.MAX_VALUE + 1000L,
        Short.MAX_VALUE - 1L,
        Short.MAX_VALUE,
        Short.MAX_VALUE + 10L,
        Short.MAX_VALUE + 1000L,
        Character.MAX_VALUE - 1L,
        Character.MAX_VALUE,
        Character.MAX_VALUE + 10L,
        Character.MAX_VALUE + 1000L,
        Integer.MAX_VALUE - 1,
        Integer.MAX_VALUE,
        Integer.MAX_VALUE + 10L,
        Integer.MAX_VALUE + 1000L,
        Long.MAX_VALUE - 1,
        Long.MAX_VALUE
    };
    long[] values = {
        Long.MIN_VALUE,
        Long.MIN_VALUE + 1L,
        Integer.MIN_VALUE,
        Integer.MIN_VALUE + 1L,
        Short.MIN_VALUE,
        Short.MIN_VALUE + 1L,
        Byte.MIN_VALUE,
        Byte.MIN_VALUE + 1L,
        -8L,
        -4L,
        -2L,
        -1L,
        Character.MIN_VALUE, // 0L
        Character.MIN_VALUE + 1L, // 1L
        2L,
        4L,
        8L,
        Byte.MAX_VALUE - 1L,
        Byte.MAX_VALUE,
        Short.MAX_VALUE - 1L,
        Short.MAX_VALUE,
        Character.MAX_VALUE - 1L,
        Character.MAX_VALUE,
        Integer.MAX_VALUE - 1L,
        Integer.MAX_VALUE,
        Long.MAX_VALUE - 1L,
        Long.MAX_VALUE
    };

    /** Contains a Range for every combination of values in rangeBounds. */
    Range[] ranges;

    static final long INT_WIDTH = (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE + 1;

    static final long SHORT_WIDTH = Short.MAX_VALUE - Short.MIN_VALUE + 1;

    static final long BYTE_WIDTH = Byte.MAX_VALUE - Byte.MIN_VALUE + 1;

    static final long CHAR_WIDTH = Character.MAX_VALUE - Character.MIN_VALUE + 1;

    public RangeTest() {
        // Initialize the ranges list to every combination of values in rangeBounds.
        List<Range> rangesList = new ArrayList<>();
        for (long lowerbound : rangeBounds) {
            for (long upperbound : rangeBounds) {
                if (lowerbound <= upperbound) {
                    rangesList.add(Range.create(lowerbound, upperbound));
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
            Range.ignoreOverflow = false;
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
    public void testIntRange() {
        for (Range range : ranges) {
            Range result = range.intRange();
            for (long value : values) {
                if (value < range.from + INT_WIDTH
                        && value > range.to - INT_WIDTH
                        && (Math.abs(range.from) - 1) / Integer.MIN_VALUE
                                == (Math.abs(range.to) - 1) / Integer.MIN_VALUE) {
                    // filter out test data that would cause Range.intRange to return INT_EVERYTHING
                    int intValue = (int) value;
                    assert range.contains(value) && result.contains(intValue)
                                    || !range.contains(value) && !result.contains(intValue)
                            : String.format(
                                    "Range.intRange failure: %s => %s; witness = %s",
                                    range, result, intValue);
                }
            }
        }
    }

    @Test
    public void testShortRange() {
        for (Range range : ranges) {
            Range result = range.shortRange();
            for (long value : values) {
                if (value < range.from + SHORT_WIDTH
                        && value > range.to - SHORT_WIDTH
                        && (Math.abs(range.from) - 1) / Short.MIN_VALUE
                                == (Math.abs(range.to) - 1) / Short.MIN_VALUE) {
                    // filter out test data that would cause Range.shortRange to return
                    // SHORT_EVERYTHING
                    short shortValue = (short) value;
                    assert range.contains(value) && result.contains(shortValue)
                                    || !range.contains(value) && !result.contains(shortValue)
                            : String.format(
                                    "Range.shortRange failure: %s => %s; witness = %s",
                                    range, result, shortValue);
                }
            }
        }
    }

    @Test
    public void testCharRange() {
        Range.ignoreOverflow = false;
        for (Range range : ranges) {
            Range result = range.charRange();
            for (long value : values) {
                if (value < range.from + CHAR_WIDTH
                        && value > range.to - CHAR_WIDTH
                        && (Math.abs(range.from + Short.MIN_VALUE) - 1) / Short.MIN_VALUE
                                == (Math.abs(range.to + Short.MIN_VALUE) - 1) / Short.MIN_VALUE) {
                    // filter out test data that would cause Range.CharRange to return
                    // CHAR_EVERYTHING
                    // char range interval is a right shift of the short range interval
                    char charValue = (char) value;
                    assert range.contains(value) && result.contains(charValue)
                                    || !range.contains(value) && !result.contains(charValue)
                            : String.format(
                                    "Range.byteRange failure: %s => %s; witness = %s",
                                    range, result, charValue);
                }
            }
        }
    }

    @Test
    public void testByteRange() {
        for (Range range : ranges) {
            Range result = range.byteRange();
            for (long value : values) {
                if (value < range.from + BYTE_WIDTH
                        && value > range.to - BYTE_WIDTH
                        && (Math.abs(range.from) - 1) / Byte.MIN_VALUE
                                == (Math.abs(range.to) - 1) / Byte.MIN_VALUE) {
                    // filter out test data that would cause Range.ByteRange to return
                    // BYTE_EVERYTHING
                    byte byteValue = (byte) value;
                    assert range.contains(value) && result.contains(byteValue)
                                    || !range.contains(value) && !result.contains(byteValue)
                            : String.format(
                                    "Range.byteRange failure: %s => %s; witness = %s",
                                    range, result, byteValue);
                }
            }
        }

        Range r1 = Range.create(5, 1000);
        Range r2 = Range.create(1024 + 17, 1024 + 22);
        Range r3 = Range.create(5, Byte.MAX_VALUE + 2);

        Range.ignoreOverflow = true;

        assert r1.byteRange().equals(Range.create(5, Byte.MAX_VALUE));
        assert r2.byteRange().equals(Range.create(Byte.MAX_VALUE, Byte.MAX_VALUE));
        assert r3.byteRange().equals(Range.create(5, Byte.MAX_VALUE));

        Range.ignoreOverflow = false;

        assert r1.byteRange().equals(Range.BYTE_EVERYTHING);
        assert r2.byteRange().equals(Range.create(17, 22));
        assert r3.byteRange().equals(Range.BYTE_EVERYTHING);
    }

    @Test
    public void testUnion() {
        for (Range range1 : ranges) {
            for (Range range2 : ranges) {
                Range result = range1.union(range2);
                for (long value : values) {
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
        assert Range.create(1, 2).divide(Range.create(0, 0)) == Range.NOTHING;
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
        assert Range.create(1, 2).remainder(Range.create(0, 0)) == Range.NOTHING;
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                if (re2.element == 0) {
                    continue;
                }
                Range result = re1.range.remainder(re2.range);
                Long witness = re1.element % re2.element;
                assert result.contains(witness)
                        : String.format(
                                "Range.remainder failure: %s %s => %s; witnesses %s %% %s => %s",
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
    public void testUnsignedShiftRight() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                Range result = re1.range.unsignedShiftRight(re2.range);
                if (re1.range.from >= 0) {
                    assert result.contains(re1.element >>> re2.element)
                            : String.format(
                                    "Range.unsignedShiftRight failure: %s %s => %s; witnesses %s >> %s => %s",
                                    re1.range,
                                    re2.range,
                                    result,
                                    re1.element,
                                    re2.element,
                                    re1.element >> re2.element);
                } else {
                    assert result == Range.EVERYTHING;
                }
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
    public void testBitwiseAnd() {
        for (RangeAndElement re1 : rangeAndElements()) {
            for (RangeAndElement re2 : rangeAndElements()) {
                Range result1 = re1.range.bitwiseAnd(re2.range);
                Range result2 = re2.range.bitwiseAnd(re1.range);
                if (re1.range.isConstant() || re2.range.isConstant()) {
                    Long witness = re1.element & re2.element;
                    assert result1.from == result2.from;
                    assert result1.to == result2.to;
                    assert result1.contains(witness)
                            : String.format(
                                    "Range.bitwiseAnd failure: %s %s => %s; witnesses %s & %s => %s",
                                    re1.range,
                                    re2.range,
                                    result1,
                                    re1.element,
                                    re2.element,
                                    witness);
                } else {
                    assert result1 == Range.EVERYTHING;
                    assert result2 == Range.EVERYTHING;
                }
            }
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

    @Test
    public void testFactoryLongLong() {
        Assert.assertEquals((long) 1, Range.create(1, 2).from);
        Assert.assertEquals((long) 2, Range.create(1, 2).to);
    }

    @Test
    public void testFactoryList() {
        Assert.assertEquals((long) 1, Range.create(Arrays.asList(1, 2, 3)).from);
        Assert.assertEquals((long) 3, Range.create(Arrays.asList(1, 2, 3)).to);
        Assert.assertEquals((long) 1, Range.create(Arrays.asList(3, 2, 1)).from);
        Assert.assertEquals((long) 3, Range.create(Arrays.asList(3, 2, 1)).to);
        Assert.assertEquals(Range.NOTHING, Range.create(Collections.<Integer>emptyList()));
        Assert.assertTrue(Range.NOTHING == Range.create(Collections.<Integer>emptyList()));
    }

    @Test
    public void testFactoryTypeKind() {
        Assert.assertEquals(Range.BYTE_EVERYTHING, Range.create(TypeKind.BYTE));
        Assert.assertEquals(Range.INT_EVERYTHING, Range.create(TypeKind.INT));
        Assert.assertEquals(Range.SHORT_EVERYTHING, Range.create(TypeKind.SHORT));
        Assert.assertEquals(Range.CHAR_EVERYTHING, Range.create(TypeKind.CHAR));
        Assert.assertEquals(Range.LONG_EVERYTHING, Range.create(TypeKind.LONG));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryTypeKindFailure() {
        Range.create(TypeKind.FLOAT);
    }
}
