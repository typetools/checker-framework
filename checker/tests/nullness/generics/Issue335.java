// Test case for Issue 335:
// https://github.com/typetools/checker-framework/issues/335

import org.checkerframework.checker.nullness.qual.Nullable;

class Pair<A, B> {
    static <C, D> Pair<C, D> of(@Nullable C first, @Nullable D second) {
        throw new RuntimeException();
    }
}

class Optional<S> {
    static <T> Optional<T> of(T reference) {
        throw new RuntimeException();
    }
}

public class Issue335 {
    Optional<Pair<String, String>> m(String one, String two) {
        return Optional.of(Pair.of(one, two));
    }
}
