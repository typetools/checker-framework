import checkers.nullness.quals.*;

public class LazyInitialization {
    @Nullable Object nullable;
    @NonNull  Object nonnull;
    @LazyNonNull Object lazy;
    @LazyNonNull Object lazy2 = null;
    final @Nullable Object lazy3;

    public LazyInitialization(@Nullable Object arg) {
        lazy3 = arg;
    }

    void randomMethod() { }

    void testAssignment() {
        lazy = "m";
        lazy = null;    // null
    }

    void testLazyBeingNull() {
        nullable.toString(); // error
        nonnull.toString();
        lazy.toString();    // error
        lazy3.toString(); // error
    }

    void testAfterInvocation() {
        nullable = "m";
        nonnull = "m";
        lazy = "m";
        if (lazy3 == null)
            return;

        randomMethod();

        nullable.toString();    // error
        nonnull.toString();
        lazy.toString();
        lazy3.toString();
    }

}
