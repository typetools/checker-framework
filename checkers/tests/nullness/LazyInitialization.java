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
        //:: (assignment.type.incompatible)
        lazy = null;    // null
    }

    void testLazyBeingNull() {
        //:: (dereference.of.nullable)
        nullable.toString(); // error
        nonnull.toString();
        //:: (dereference.of.nullable)
        lazy.toString();    // error
        //:: (dereference.of.nullable)
        lazy3.toString(); // error
    }

    void testAfterInvocation() {
        nullable = "m";
        nonnull = "m";
        lazy = "m";
        if (lazy3 == null)
            return;

        randomMethod();

        //:: (dereference.of.nullable)
        nullable.toString();    // error
        nonnull.toString();
        lazy.toString();
        lazy3.toString();
    }

    @LazyNonNull
    private double [] intersect = null;

    public void check_modified(double[] a, int count) {
        if (intersect!=null) {
            double @NonNull [] nnda = intersect;
        }
    }

}
