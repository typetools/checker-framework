import org.checkerframework.checker.upperbound.qual.*;

class RefineLT {
    void testLTL() {
        // The reason for the parsing is so that the Value Checker
        // can't figure it out but normal humans can.

        //:: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int a = Integer.parseInt("1");

        //:: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int a3 = Integer.parseInt("3");

        int b = 2;
        if (b < a3) {
            @LTLengthOf("arr") int c = b;
        }
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int c1 = b;

        if (b < a3) {
            int potato = 7;
        } else {
            //:: error: (assignment.type.incompatible)
            @LTLengthOf("arr") int d = b;
        }
    }

    void testLTEL() {
        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf("arr") int a = Integer.parseInt("1");

        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf("arr") int a3 = Integer.parseInt("3");

        int b = 2;
        if (b < a3) {
            @LTEqLengthOf("arr") int c = b;
        }
        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf("arr") int c1 = b;

        if (b < a) {
            int potato = 7;
        } else {
            //:: error: (assignment.type.incompatible)
            @LTEqLengthOf("arr") int d = b;
        }
    }
}
