import org.checkerframework.checker.nullness.qual.*;

class GenericsBounds4 {
    class Collection1<E extends @Nullable Object> {
        public void add(E elt) {
            // :: error: (dereference.of.nullable)
            elt.hashCode();
        }
    }

    Collection1<? extends @Nullable Object> f1 = new Collection1<@NonNull Object>();
    // :: error: (assignment.type.incompatible)
    Collection1<@Nullable ? extends @Nullable Object> f2 = new Collection1<@NonNull Object>();
    Collection1<@Nullable ? extends @Nullable Object> f3 = new Collection1<@Nullable Object>();

    void bad() {
        // This has to be forbidden, because f1 might refer to a
        // collection that has NonNull as type argument.
        // :: error: (argument.type.incompatible)
        f1.add(null);

        // This is forbidden by the Java type rules:
        // f1.add(new Object());

        // ok
        f3.add(null);
    }
}
