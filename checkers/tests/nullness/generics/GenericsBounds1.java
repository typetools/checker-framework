import checkers.nullness.quals.*;

interface List<E extends @Nullable Object> {
    void add(E p);
}

/*
 * Illustrate a problem with annotations on type variables.
 * The annotation on the upper bound of a type variable is confused with an
 * annotation on the type variable itself.
 */
public class GenericsBounds1<X extends @Nullable Object> {
    void m1(@NonNull List<X> g1, @NonNull List<@Nullable X> g2) {
        //:: error: (assignment.type.incompatible)
        g1 = null;
        //:: error: (argument.type.incompatible)
        g1.add(null);

        //:: error: (assignment.type.incompatible)
        g2 = null;
        g2.add(null);

        //:: error: (assignment.type.incompatible)
        g2 = g1;
        g2.add(null);
    }
}

