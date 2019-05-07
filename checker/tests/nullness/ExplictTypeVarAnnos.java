import org.checkerframework.checker.nullness.qual.Nullable;

public class ExplictTypeVarAnnos<E extends @Nullable Object> {
    interface Consumer<A extends @Nullable Object> {}

    public static <B extends @Nullable Object> Consumer<B> cast(
            final @Nullable Consumer<? super B> consumer) {
        throw new RuntimeException();
    }

    public static <C extends @Nullable Object> Consumer<C> getConsumer() {
        Consumer<@Nullable Object> nullConsumer = null;
        Consumer<C> result = ExplictTypeVarAnnos.<C>cast(nullConsumer);
        return result;
    }

    public Consumer<E> getConsumer2() {
        Consumer<@Nullable Object> nullConsumer = null;
        Consumer<E> result = ExplictTypeVarAnnos.<E>cast(nullConsumer);
        return result;
    }
}
