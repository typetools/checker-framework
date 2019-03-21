// Test case for Issue #1209
// https://github.com/typetools/checker-framework/issues/1209

import org.checkerframework.checker.signedness.qual.PolySigned;

public class PolymorphicReturnType {

    // :: error: (return.type.incompatible)
    public @PolySigned byte get() {
        return 0;
    }
}
