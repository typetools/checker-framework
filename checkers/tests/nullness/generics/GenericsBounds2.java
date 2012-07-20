import checkers.nullness.quals.*;

/*
 * Illustrate a problem with annotations on type variables.
 */
public class GenericsBounds2<X extends @Nullable Object> {
    void m1(X @NonNull[] a1, @Nullable X @NonNull[] a2) {
        //:: error: (assignment.type.incompatible)
        a1 = null;
        //:: error: (assignment.type.incompatible)
        a1[0] = null;

        //:: error: (assignment.type.incompatible)
        a2 = null;
        a2[0] = null;

        // This error is expected when arrays are invariant.
        // Currently, this error is not raised.
        //TODOINVARR:: error: (assignment.type.incompatible)
        a2 = a1;
        a2[0] = null;
    }

    void aaa(@Nullable Object[] p1, @NonNull Object[] p2) {
        // This one is only expected when we switch the default for arrays to be invariant.
        //TODOINVARR:: error: (assignment.type.incompatible)
        p1 = p2;
    }
}

