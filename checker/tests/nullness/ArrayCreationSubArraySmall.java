// Test case for issue #599:
// https://github.com/typetools/checker-framework/issues/599

// @skip-test Commented out until the bug is fixed

import org.checkerframework.checker.nullness.qual.*;

public class ArrayCreationSubArraySmall {
    void m(@Nullable Object[] a) {
        Object o3a = new Object[][] {a};
    }
}
