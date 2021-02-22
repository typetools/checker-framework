import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class RefineLTE {
    int[] arr = {1};

    void testLTL(@LTLengthOf("arr") int test) {
        // The reason for the parsing is so that the Value Checker
        // can't figure it out but normal humans can.

        // :: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int a = Integer.parseInt("1");

        // :: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int a3 = Integer.parseInt("3");

        int b = 2;
        if (b <= test) {
            @LTLengthOf("arr") int c = b;
        }
        // :: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int c1 = b;

        if (b <= a) {
            int potato = 7;
        } else {
            // :: error: (assignment.type.incompatible)
            @LTLengthOf("arr") int d = b;
        }
    }

    void testLTEL(@LTEqLengthOf("arr") int test) {
        // :: error: (assignment.type.incompatible)
        @LTEqLengthOf("arr") int a = Integer.parseInt("1");

        // :: error: (assignment.type.incompatible)
        @LTEqLengthOf("arr") int a3 = Integer.parseInt("3");

        int b = 2;
        if (b <= test) {
            @LTEqLengthOf("arr") int c = b;
        }
        // :: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int c1 = b;

        if (b <= a) {
            int potato = 7;
        } else {
            // :: error: (assignment.type.incompatible)
            @LTLengthOf("arr") int d = b;
        }
    }
}
