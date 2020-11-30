import org.checkerframework.checker.nullness.qual.*;

// Field f needs to be set, because the upper bound is @Initialized
// :: error: (initialization.fields.uninitialized)
class Param<T extends @NonNull Object> {
    T f;

    void foo() {
        // Valid, because upper bound is @Initialized @NonNull
        f.toString();
    }
}

// :: error: (type.argument.type.incompatible)
class Invalid<S extends Param<@Nullable Object>> {
    void bar(S s) {
        s.foo();
    }

    // :: error: (type.argument.type.incompatible)
    <M extends Param<@Nullable Object>> void foobar(M p) {}
}

interface ParamI<T extends @NonNull Object> {}

class Invalid2<
        S extends
                Number &
                        // :: error: (type.argument.type.incompatible)
                        ParamI<@Nullable Object>> {}

class Invalid3 {
    <
                    M extends
                            Number &
                                    // :: error: (type.argument.type.incompatible)
                                    ParamI<@Nullable Object>>
            void foobar(M p) {}
}
