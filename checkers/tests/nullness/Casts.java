import checkers.nullness.quals.*;

public class Casts {

    void test(String nonNullParam) {
        Object lc1 = (Object) nonNullParam;
        lc1.toString();

        String nullable = null;
        Object lc2 = (Object) nullable;
        //:: error: (dereference.of.nullable)
        lc2.toString(); // error
    }

    void testBoxing() {
        Integer b = null;
        //:: error: (assignment.type.incompatible)
        int i = b;
        //:: error: (unboxing.of.nullable)
        Object o = (int)b;
    }

    void testUnsafeCast(@Nullable Object x) {
        //:: warning: (cast.unsafe)
        @NonNull Object y = (@NonNull Object) x;
        y.toString();
    }

    void testUnsafeCastArray1(@Nullable Object[] x) {
        // Warning only with -Alint:cast:strict.
        //TODO:: warning: (cast.unsafe)
        @NonNull Object[] y = (@NonNull Object[]) x;
        y[0].toString();
    }

    void testUnsafeCastArray2(@NonNull Object x) {
        // We don't know about the component type of x -> warn
        // Warning only with -Alint:cast:strict.
        //TODO:: warning: (cast.unsafe)
        @NonNull Object[] y = (@NonNull Object[]) x;
        y[0].toString();
    }

    void testUnsafeCastList1(java.util.ArrayList<@Nullable Object> x) {
        // Warning only with -Alint:cast:strict.
        //TODO:: warning: (cast.unsafe)
        java.util.List<@NonNull Object> y = (java.util.List<@NonNull Object>) x;
        y.get(0).toString();
        //TODO:: warning: (cast.unsafe)
        java.util.List<@NonNull Object> y2 = (java.util.ArrayList<@NonNull Object>) x;
        java.util.List<@Nullable Object> y3 = (java.util.List<@Nullable Object>) x;
    }

    void testUnsafeCastList2(java.util.List<@Nullable Object> x) {
        java.util.List<@Nullable Object> y = (java.util.ArrayList<@Nullable Object>) x;
        // Warning only with -Alint:cast:strict.
        //TODO:: warning: (cast.unsafe)
        java.util.List<@NonNull Object> y2 = (java.util.ArrayList<@NonNull Object>) x;
    }

    void testUnsafeCastList3(@NonNull Object x) {
        // Warning only with -Alint:cast:strict.
        //TODO:: warning: (cast.unsafe)
        java.util.List<@Nullable Object> y = (java.util.List<@Nullable Object>) x;
        //TODO:: warning: (cast.unsafe)
        java.util.List<@NonNull Object> y2 = (java.util.ArrayList<@NonNull Object>) x;
    }

    void testSuppression(@Nullable Object x) {
        //:: error: (assignment.type.incompatible)
        @NonNull String s1 = (String) x;
        @SuppressWarnings("nullness")
        @NonNull String s2 = (String) x;
    }

    class Generics<T> {
        T t;
        @Nullable T nt;
        Generics(T t) {
            this.t = t;
            this.nt = t;
        }
        void m() {
            //:: error: (assignment.type.incompatible)
            t = (@Nullable T) null;
            nt = (@Nullable T) null;
            //:: warning: (cast.unsafe)
            t = (T) null;
            //:: warning: (cast.unsafe)
            nt = (T) null;
        }
    }
}
