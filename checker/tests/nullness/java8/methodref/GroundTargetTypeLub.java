import org.checkerframework.checker.nullness.qual.*;

interface Supplier<T extends @NonNull Object> {
    T supply();
}

interface Supplier2<T extends @Nullable Object> {
    T supply();
}

class GroundTargetType {

    static @Nullable Object myMethod() {
        return null;
    }

    // :: error: (type.argument.type.incompatible)
    Supplier<? extends @Nullable Object> fn = GroundTargetType::myMethod;
    // :: error: (methodref.return.invalid)
    Supplier<? extends @NonNull Object> fn2 = GroundTargetType::myMethod;

    // Supplier2
    // :: error: (methodref.return.invalid)
    Supplier2<? extends @NonNull Object> fn3 = GroundTargetType::myMethod;
}
