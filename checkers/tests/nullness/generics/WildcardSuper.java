import checkers.nullness.quals.*;
import checkers.quals.*;

class WildcardSuper {

    void testWithSuper(Cell<? super @NonNull String> cell) {
        // This is valid because the default upper bound is NonNull
        cell.get().toString();
    }

    void testWithContradiction(Cell<? super @Nullable String> cell) {
        // This is actually valid, because it's a contradiction, b/c
        // the implicit upper bound is NonNull.
        // We are free to do anything, as the method is not callable.
        // TODO: test whether all calls of method fail.
        cell.get().toString();
    }

    @DefaultQualifier("Nullable")
    void testWithImplicitNullable(@NonNull Cell<? super @NonNull String> cell) {
        //:: error: (dereference.of.nullable)
        cell.get().toString();
    }

    void testWithExplicitNullable(Cell<@Nullable ? extends @Nullable String> cell) {
        //:: error: (dereference.of.nullable)
        cell.get().toString();
    }

    void testWithDoubleNullable(Cell<@Nullable ? extends @Nullable String> cell) {
        //:: error: (dereference.of.nullable)
        cell.get().toString();
    }

    class Cell<E extends @Nullable Object> {
        E get() { throw new RuntimeException(); }
    }
}