import checkers.nullness.quals.*;

public class LazyInitialization {
    @Nullable Object nullable;
    @NonNull  Object nonnull;
    @LazyNonNull Object lazy;
    @LazyNonNull Object lazy2 = null;
    final @Nullable Object lazy3;

    public LazyInitialization(@Nullable Object arg) {
        lazy3 = arg;
        nonnull = new Object();
    }

    void randomMethod() { }

    void testAssignment() {
        lazy = "m";
        //:: error: (assignment.type.incompatible)
        lazy = null;    // null
    }

    void testLazyBeingNull() {
        //:: error: (dereference.of.nullable)
        nullable.toString(); // error
        nonnull.toString();
        //:: error: (dereference.of.nullable)
        lazy.toString();    // error
        //:: error: (dereference.of.nullable)
        lazy3.toString(); // error
    }

    void testAfterInvocation() {
        nullable = "m";
        nonnull = "m";
        lazy = "m";
        if (lazy3 == null)
            return;

        randomMethod();

        //:: error: (dereference.of.nullable)
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

    class PptRelation1 {
        // TODO: This @SuppressWarnings should not be necessary.
        // See bug 107: http://code.google.com/p/checker-framework/issues/detail?id=107
        @SuppressWarnings("nullness")
        public void init_hierarchy_new (PptTopLevel ppt, Object eq) {
            ppt.equality_view = eq;
            ppt.equality_view.toString();
        }
    }
    class PptTopLevel {
        public @LazyNonNull Object equality_view;
    }

    class PptRelation2 {
        public @LazyNonNull Object equality_view2;
        public void init_hierarchy_new (PptRelation2 pr, Object eq) {
            pr.equality_view2 = eq;
            pr.equality_view2.toString();
        }
    }

}
