import checkers.nullness.quals.*;
import checkers.quals.*;

class WildcardSuper {

    void testWithSuper(Cell<? super @NonNull String> cell) {
        // this is value because the default upper bound is NonNull
        cell.get().toString();
    }

    void testWithContradiction(Cell<? super @Nullable String> cell) {
        // this actually valid, because it's a contradition!
        // we are free to do anything
        cell.get().toString();
    }

    @DefaultQualifier("Nullable")
    void testWithImplicitNullable(@NonNull Cell<? super @NonNull String> cell) {
        //:: (dereference.of.nullable)
        cell.get().toString();
    }

    void testWithExplicitNullable(Cell<@Nullable ? super @NonNull String> cell) {
        //:: (dereference.of.nullable)
        cell.get().toString();
    }

    void testWithDoubleNullable(Cell<@Nullable ? super @Nullable String> cell) {
        //:: (dereference.of.nullable)
        cell.get().toString();
    }

    class Cell<E extends @Nullable Object> {
        E get() { throw new RuntimeException(); }
    }
}