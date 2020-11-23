import org.checkerframework.checker.nullness.qual.Nullable;

class Issue3754 {
    interface Supplier<T extends @Nullable Object, U extends T> {
        U get();
    }

    Object x(Supplier<? extends Object, ?> bar) {
        return bar.get();
    }

    interface Supplier2<U extends T, T extends @Nullable Object> {
        U get();
    }

    Object x(Supplier2<?, ? extends Object> bar) {
        return bar.get();
    }
}
