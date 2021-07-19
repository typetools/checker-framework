import org.checkerframework.checker.nullness.qual.*;

public class Wellformed {
    // :: error: (type.invalid.conflicting.annos)
    @NonNull @Nullable Object f = null;

    // :: error: (type.invalid.conflicting.annos)
    class Gen1a<T extends @NonNull @Nullable Object> {}

    class Gen1b {
        // :: error: (type.invalid.conflicting.annos)
        <T extends @NonNull @Nullable Object> void m(T p) {}
        // :: error: (type.invalid.conflicting.annos)
        <@NonNull @Nullable T> void m2(T p) {}
    }
    // :: error: (type.invalid.conflicting.annos)
    class Gen1c<@NonNull @Nullable TTT> {}

    class Gen2a<@Nullable T> {}

    // :: error: (bound.type.incompatible)
    class Gen2b<@Nullable T extends Object> {}

    // :: error: (bound.type.incompatible)
    class Gen2c<@Nullable T extends @NonNull Object> {}

    class Gen3a<T> {
        @Nullable T f;

        @Nullable T get() {
            return null;
        }
    }

    class Gen3b<T extends @NonNull Object> {
        @Nullable T f;

        @Nullable T get() {
            return null;
        }
    }

    class Gen4<T extends @Nullable Object> {
        // :: error: (initialization.field.uninitialized)
        @NonNull T f;

        @NonNull T get() {
            throw new RuntimeException();
        }

        void set(@NonNull T p) {}
    }

    class Gen5a<T extends @Nullable Object> {}

    class Gen5b<S> extends Gen5a<@Nullable Object> {}

    class Gen5c<S> extends Gen5a<@Nullable S> {}

    class Gen6a<T extends Object> {}
    // :: error: (type.argument.type.incompatible)
    class Gen6b<S> extends Gen6a<@Nullable Object> {}
    // :: error: (type.argument.type.incompatible)
    class Gen6c<S> extends Gen6a<@Nullable S> {}
}
