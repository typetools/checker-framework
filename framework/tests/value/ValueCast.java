// Test case for issue 1299: https://github.com/typetools/checker-framework/issues/1299

import org.checkerframework.common.value.qual.*;

class ValueCast {
    void testShort_plus(@IntRange(from = 0) short x) {
        @IntRange(from = 1, to = Short.MAX_VALUE + 1) int y = x + 1;
    }

    void testIntFrom(@IntRange(from = 0) int x) {
        @IntRange(from = 0, to = Integer.MAX_VALUE) long y = x;
    }

    void testShortFrom(@IntRange(from = 0) short x) {
        @IntRange(from = 0, to = Short.MAX_VALUE) int y = x;
    }

    void testByteFrom(@IntRange(from = 0) byte x) {
        @IntRange(from = 0, to = Byte.MAX_VALUE) int y = x;
    }

    void testIntTo(@IntRange(to = 0) int x) {
        @IntRange(to = 0, from = Integer.MIN_VALUE) long y = x;
    }

    void testShortTo(@IntRange(to = 0) short x) {
        @IntRange(to = 0, from = Short.MIN_VALUE) int y = x;
    }

    void testByteTo(@IntRange(to = 0) byte x) {
        @IntRange(to = 0, from = Byte.MIN_VALUE) int y = x;
    }
}
