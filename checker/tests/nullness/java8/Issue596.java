// Test case for Issue 596:
// https://github.com/typetools/checker-framework/issues/596
// @below-java8-jdk-skip-test

import java.util.concurrent.atomic.AtomicReference;
import org.checkerframework.checker.nullness.qual.*;

class Issue596 {

    private static String getOrEmpty(AtomicReference<String> ref) {
        return Optional.fromNullable(ref.get()).or("");
    }
}

// From Google Guava
class Optional<T> {

    public static <T> Optional<T> fromNullable(@Nullable T nullableReference) {
        return new Optional<T>();
    }

    public T or(T defaultValue) {
        return defaultValue;
    }
}
