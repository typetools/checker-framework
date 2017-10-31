// Test case for Issue 1555
// https://github.com/typetools/checker-framework/issues/1555

import org.checkerframework.checker.nullness.NullnessUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

class Issue1555 {

    private @MonotonicNonNull String x;

    String test() {
        return NullnessUtil.castNonNull(x);
    }
}
