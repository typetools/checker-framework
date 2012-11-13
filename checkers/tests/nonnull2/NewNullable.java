import checkers.nullness.quals.*;

class NewNullable {
    Object o = new Object();
    Object nn = new @NonNull Object();
    //:: warning: (new.class.type.invalid)
    @Nullable Object lazy = new @LazyNonNull Object();
    //:: warning: (new.class.type.invalid)
    @Nullable Object poly = new @PolyNull Object();
    //:: warning: (new.class.type.invalid)
    @Nullable Object nbl = new @Nullable Object();
}
