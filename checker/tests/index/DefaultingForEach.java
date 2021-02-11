// Test case for issue #4248: https://github.com/typetools/checker-framework/issues/4248.
// This test exposed a crash in the original version of the fix.

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.framework.qual.DefaultQualifier;

class DefaultForEach {

    @DefaultQualifier(NonNegative.class)
    static int[] foo() {
        throw new RuntimeException();
    }

    void bar() {
        for (Integer p : foo()) {
            // :: error: assignment.type.incompatible
            @Positive int x = p;
            @NonNegative int y = p;
        }
    }
}
