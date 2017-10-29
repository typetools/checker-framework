import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;

class WildcardSuper {

    void testWithSuper(Cell<? super @NonNull String> cell) {
        // TODO: Address comments.  Since ? is explicitly lower bounded, I have made a judgment that
        // it should be implicitly upper bounded.
        // This is valid because the default upper bound is NonNull
        // :: error: (dereference.of.nullable)
        cell.get().toString();
    }

    // TODO: THIS SHOULD JUST ISSUE A WARNING, WHY WOULD PEOPLE WANT TO WRITE CONTRADICTING BOUNDS?
    void testWithContradiction(Cell<? super @Nullable String> cell) {
        // This is actually valid, because it's a contradiction, b/c
        // the implicit upper bound is NonNull.
        // We are free to do anything, as the method is not callable.
        // TODO: test whether all calls of method fail.
        // :: error: (dereference.of.nullable)
        cell.get().toString();
    }

    @DefaultQualifier(Nullable.class)
    void testWithImplicitNullable(@NonNull Cell<? super @NonNull String> cell) {
        // :: error: (dereference.of.nullable)
        cell.get().toString();
    }

    void testWithExplicitNullable(Cell<@Nullable ? extends @Nullable String> cell) {
        // :: error: (dereference.of.nullable)
        cell.get().toString();
    }

    void testWithDoubleNullable(Cell<@Nullable ? extends @Nullable String> cell) {
        // :: error: (dereference.of.nullable)
        cell.get().toString();
    }

    class Cell<E extends @Nullable Object> {
        E get() {
            throw new RuntimeException();
        }
    }
}
