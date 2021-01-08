// Test case for issue 2815: https://github.com/typetools/checker-framework/issues/2815

import org.checkerframework.common.value.qual.*;

public class ValueWrapperCast {
    void testShort_plus(@IntRange(from = 0) Short x) {
        @IntRange(from = 1, to = Short.MAX_VALUE + 1) int y = x + 1;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 1, to = Short.MAX_VALUE - 1) int z = x;
    }

    void testIntFrom(@IntRange(from = 0) Integer x) {
        @IntRange(from = 0, to = Integer.MAX_VALUE) long y = x;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int z = x;
    }

    void testShortFrom(@IntRange(from = 0) Short x) {
        @IntRange(from = 0, to = Short.MAX_VALUE) int y = x;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0, to = Short.MAX_VALUE - 1) int z = x;
    }

    void testCharFrom(@IntRange(from = 0) Character x) {
        @IntRange(from = 0, to = Character.MAX_VALUE) int y = x;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0, to = Character.MAX_VALUE - 1) int z = x;
    }

    void testByteFrom(@IntRange(from = 0) Byte x) {
        @IntRange(from = 0, to = Byte.MAX_VALUE) int y = x;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0, to = Byte.MAX_VALUE - 1) int z = x;
    }

    void testIntTo(@IntRange(to = 0) Integer x) {
        @IntRange(to = 0, from = Integer.MIN_VALUE) long y = x;
        // :: error: (assignment.type.incompatible)
        @IntRange(to = 0, from = Integer.MIN_VALUE + 1) int z = x;
    }

    void testShortTo(@IntRange(to = 0) Short x) {
        @IntRange(to = 0, from = Short.MIN_VALUE) int y = x;
        // :: error: (assignment.type.incompatible)
        @IntRange(to = 0, from = Short.MIN_VALUE + 1) int z = x;
    }

    void testCharTo(@IntRange(to = 1) Character x) {
        @IntRange(to = 1, from = Character.MIN_VALUE) int y = x;
        // :: error: (assignment.type.incompatible)
        @IntRange(to = 1, from = Character.MIN_VALUE + 1) int z = x;
    }

    void testByteTo(@IntRange(to = 0) Byte x) {
        @IntRange(to = 0, from = Byte.MIN_VALUE) int y = x;
        // :: error: (assignment.type.incompatible)
        @IntRange(to = 0, from = Byte.MIN_VALUE + 1) int z = x;
    }
}
