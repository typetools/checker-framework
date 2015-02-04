// Test case for Issue 355:
// https://code.google.com/p/checker-framework/issues/detail?id=355

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

class Test {
    static <T extends @Nullable Object> @NonNull T checkNotNull(@Nullable T sample) {
        throw new RuntimeException();
    }

    void m(List<String> xs) {
        for (String x : checkNotNull(xs)) {

        }
    }
}