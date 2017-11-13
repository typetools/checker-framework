import org.checkerframework.checker.nullness.qual.Nullable;

class WildcardSuper {
    @Nullable Object foo;

    interface Consumer<T> {
        void consume(T object);
    }

    Consumer<String> testCast(Consumer<Object> consumer) {
        return cast(consumer);
    }

    private static <T> Consumer<T> cast(final Consumer<? super T> consumer) {
        throw new RuntimeException();
    }
}
