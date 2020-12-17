import org.checkerframework.checker.nullness.qual.*;

public class GenericsBounds5 {
    class Collection1<E extends @Nullable Object> {
        public void add(E elt) {
            // This call is forbidden, because elt might be null.
            // :: error: (dereference.of.nullable)
            elt.hashCode();
        }
    }

    <@Nullable F extends @Nullable Object> void addNull1(Collection1<F> l) {
        // This call is allowed, because F is definitely @Nullable.
        l.add(null);
    }

    // Effectively, this should be the same signature as above.
    // TODO: the type "@Nullable ?" is "@Nullable ? extends @NonNull Object",
    // with the wrong extends bound.
    void addNull2(Collection1<@Nullable ? extends @Nullable Object> l) {
        // This call has to pass, like above.
        l.add(null);
    }

    <@Nullable F extends @Nullable Object> void addNull3(Collection1<F> l, F p) {
        // This call is allowed, because F is definitely @Nullable.
        l.add(null);
        l.add(p);
    }

    // :: error: (assignment.type.incompatible)
    Collection1<@Nullable ? extends @Nullable Integer> f = new Collection1<@NonNull Integer>();

    void bad(Collection1<@NonNull Integer> nnarg) {
        // These have to be forbidden, because f1 might refer to a
        // collection that has NonNull as type argument.
        // :: error: (type.argument.type.incompatible)
        addNull1(nnarg);

        // :: error: (argument.type.incompatible)
        addNull2(nnarg);

        // :: error: (type.argument.type.incompatible)
        addNull3(nnarg, Integer.valueOf(4));
    }
}
