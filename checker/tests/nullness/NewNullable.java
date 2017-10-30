import org.checkerframework.checker.nullness.qual.*;

class NewNullable {
    Object o = new Object();
    Object nn = new @NonNull Object();
    // :: warning: (new.class.type.invalid)
    @Nullable Object lazy = new @MonotonicNonNull Object();
    // :: warning: (new.class.type.invalid)
    @Nullable Object poly = new @PolyNull Object();
    // :: warning: (new.class.type.invalid)
    @Nullable Object nbl = new @Nullable Object();
}
