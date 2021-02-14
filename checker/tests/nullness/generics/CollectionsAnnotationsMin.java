import org.checkerframework.checker.nullness.qual.*;

public class CollectionsAnnotationsMin {
    static class Collection1<E extends @Nullable Object> {
        public void add(E elt) {
            // :: error: (dereference.of.nullable)
            elt.hashCode();
        }
    }

    static class PriorityQueue1<E extends @NonNull Object> extends Collection1<E> {
        public void add(E elt) {
            // dereference allowed here
            elt.hashCode();
        }
    }

    // This is allowed, as "null" cannot be added to f1
    static Collection1<? extends @Nullable Object> f1 = new PriorityQueue1<@NonNull Object>();

    // :: error: (assignment.type.incompatible)
    static Collection1<@Nullable Object> f2 = new PriorityQueue1<@NonNull Object>();

    static void addNull1(Collection1<@Nullable Object> l) {
        l.add(null);
    }

    // The upper bound on E is implicitly from Collection1
    static <E extends @Nullable Object> void addNull2(Collection1<E> l) {
        // :: error: (argument.type.incompatible)
        l.add(null);
    }

    // The upper bound on E is implicitly from Collection1
    static <E extends @Nullable Object> E addNull2b(Collection1<E> l, E p) {
        // :: error: (argument.type.incompatible)
        l.add(null);
        return p;
    }

    static <@Nullable E extends @Nullable Object> void addNull3(Collection1<E> l) {
        l.add(null);
    }

    static void bad() {
        // :: error: (argument.type.incompatible)
        addNull1(new PriorityQueue1<@NonNull Object>());

        addNull2(new PriorityQueue1<@NonNull Object>());
        addNull2b(new PriorityQueue1<@NonNull Object>(), new Object());

        // :: error: (type.argument.type.incompatible)
        addNull3(new PriorityQueue1<@NonNull Object>());

        // :: error: (argument.type.incompatible)
        f1.add(null);
    }
}
