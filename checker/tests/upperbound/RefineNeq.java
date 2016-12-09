import org.checkerframework.checker.upperbound.qual.*;

class RefineNeq {
    void testLTL() {
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int a = Integer.parseInt("1");

        int b = 1;
        if (a != b) {
            //:: error: (assignment.type.incompatible)
            @LTLengthOf("arr") int e = b;

        } else {

            @LTLengthOf("arr") int c = b;
        }
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int d = b;
    }

    void testLTEL() {
        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf("arr") int a = Integer.parseInt("1");

        int b = 1;
        if (a != b) {
            //:: error: (assignment.type.incompatible)
            @LTEqLengthOf("arr") int e = b;
        } else {
            @LTEqLengthOf("arr") int c = b;

            //:: error: (assignment.type.incompatible)
            @LTLengthOf("arr") int g = b;
        }
        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf("arr") int d = b;
    }
}
