import org.checkerframework.checker.nullness.qual.NonNull;

abstract class Issue3013<E extends @NonNull Object> {
    static class Nested {
        Nested(Issue3013<?> list) {
            for (Object o : list.asIterable()) {}
        }
    }

    abstract Iterable<E> asIterable();
}
