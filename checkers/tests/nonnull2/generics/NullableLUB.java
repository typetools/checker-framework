import checkers.nullness.quals.*;

/* Test case that illustrated an unsoundness in the unification of
 * type variables with non-type variables. The error did not previously
 * get raised, leading to a missed NPE.
 */
public class NullableLUB<T extends @Nullable Object> {
    @Nullable T nt;

    T m(boolean b, T p) {
        //:: error: (assignment.type.incompatible)
        T r1 = b ? p : null;
        nt = r1;
        return r1;
    }

    public static void main(String[] args) {
        new NullableLUB<@NonNull Object>().m(false, new Object()).toString();
    }
}
