// Test case for Issue #1209
// https://github.com/typetools/checker-framework/issues/1209

// @skip-test until the issue is fixed

import org.checkerframework.checker.signedness.qual.PolySignedness;

public class PolymorphicReturnType {

    // :: error: (some.error.goes.here)
    public @PolySignedness byte get() {
        return 0;
    }
}
