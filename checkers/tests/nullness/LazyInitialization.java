import checkers.nullness.quals.*;

public class LazyInitialization {
    @Nullable Object nullable;
    @NonNull  Object nonnull;
    @LazyNonNull Object lazy;
    @LazyNonNull Object lazy2 = null;

    void randomMethod() { }

    void testAssignment() {
        lazy = "m";
        lazy = null;    // null
    }

    void testLazyBeingNull() {
        nullable.toString(); // error
        nonnull.toString();
        lazy.toString();    // error
    }

    void testAfterInvocation() {
        nullable = "m";
        nonnull = "m";
        lazy = "m";

        randomMethod();

        nullable.toString();    // error
        nonnull.toString();
        lazy.toString();
    }
}
