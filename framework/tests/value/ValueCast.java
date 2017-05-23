// Test case for issue 1299: https://github.com/typetools/checker-framework/issues/1299

//@skip-test

import org.checkerframework.common.value.qual.*;

class ValueCast {
    void testShort_plus(@IntRange(from = 0) short x) {
        @IntRange(from = 1, to = Short.MAX_VALUE + 1) int y = x + 1;
    }
}
