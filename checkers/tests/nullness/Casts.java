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

}
