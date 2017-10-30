// Test case for Issue 314:
// https://github.com/typetools/checker-framework/issues/314

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue314 {
    <T extends @NonNull Object> List<T> m1(List<@NonNull T> l1) {
        return l1;
    }

    <T extends @Nullable Object> List<T> m2(List<@NonNull T> l1) {
        // :: error: (return.type.incompatible)
        return l1;
    }

    class Also<S extends @NonNull Object> {
        S f1;
        @NonNull S f2;

        {
            // :: error: (assignment.type.incompatible)
            f1 = f2;
            // :: error: (assignment.type.incompatible)
            f2 = f1;
        }
    }
}
