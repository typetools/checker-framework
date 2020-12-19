public class WildcardSuper {
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
