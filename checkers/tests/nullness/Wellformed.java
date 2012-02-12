import checkers.nullness.quals.*;

class Wellformed {
    //:: error: (type.invalid)
    @NonNull @Nullable Object f = null;

    //:: error: (type.invalid)
    class Gen1a<T extends @NonNull @Nullable Object> {}
    class Gen1b {
        //:: error: (type.invalid)
        <T extends @NonNull @Nullable Object> void m(T p){}
        //:: error: (type.invalid)
        <@NonNull @Nullable T> void m2(T p){}
    }
    //:: error: (type.invalid)
    class Gen1c<@NonNull @Nullable T> {}

    // Annotations on type variables override implicit and explicit
    // annotations on the bounds.
    class Gen2a<@Nullable T> {}
    class Gen2b<@Nullable T extends Object> {}
    class Gen2c<@Nullable T extends @NonNull Object> {}

    class Gen3a<T> {
        @Nullable T f;
        @Nullable T get() { return null; }
    }
    class Gen3b<T extends @NonNull Object> {
        @Nullable T f;
        @Nullable T get() { return null; }
    }

    //:: error: (fields.uninitialized)
    class Gen4<T extends @Nullable Object> {
        @NonNull T f;
        @NonNull T get() { throw new RuntimeException(); }
        void set(@NonNull T p) { }
    }

    class Gen5a<T extends @Nullable Object> {}
    class Gen5b<S> extends Gen5a<@Nullable Object> {}
    class Gen5c<S> extends Gen5a<@Nullable S> {}

    class Gen6a<T> {}
    //:: error: (type.argument.type.incompatible)
    class Gen6b<S> extends Gen6a<@Nullable Object> {}
    //:: error: (type.argument.type.incompatible)
    class Gen6c<S> extends Gen6a<@Nullable S> {}
}
