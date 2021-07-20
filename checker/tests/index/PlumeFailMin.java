// Test case for eisop Issue 22:
// https://github.com/eisop/checker-framework/issues/22

// @skip-test until the issue is fixed

import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.common.value.qual.MinLen;

abstract class PlumeFailMin {
    void ok() {
        String @MinLen(1) [] args = getArrayOk();
        @IndexOrHigh("args") int x = 1;
    }

    abstract String @MinLen(1) [] getArrayOk();

    void fail() {
        // Workaround by casting.
        @SuppressWarnings({"index", "value"})
        String @MinLen(1) [] args = (String @MinLen(1) []) getArrayFail();
        @IndexOrHigh("args") int x = 1;
    }

    abstract String[] getArrayFail();
}
