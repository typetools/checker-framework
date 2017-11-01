import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

class RefineEq {
    int[] arr = {1};

    void testLTL(@LTLengthOf("arr") int test) {
        // :: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int a = Integer.parseInt("1");

        int b = 1;
        if (test == b) {
            @LTLengthOf("arr") int c = b;

        } else {
            // :: error: (assignment.type.incompatible)
            @LTLengthOf("arr") int e = b;
        }
        // :: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int d = b;
    }

    void testLTEL(@LTEqLengthOf("arr") int test) {
        // :: error: (assignment.type.incompatible)
        @LTEqLengthOf("arr") int a = Integer.parseInt("1");

        int b = 1;
        if (test == b) {
            @LTEqLengthOf("arr") int c = b;

            // :: error: (assignment.type.incompatible)
            @LTLengthOf("arr") int g = b;
        } else {
            // :: error: (assignment.type.incompatible)
            @LTEqLengthOf("arr") int e = b;
        }
        // :: error: (assignment.type.incompatible)
        @LTEqLengthOf("arr") int d = b;
    }
}
