import org.checkerframework.checker.upperbound.qual.*;

class RefineNeq {
    void testLTL() {
        //:: error: (assignment.type.incompatible)
        @LtLength("arr")
        int a = Integer.parseInt("1");

        int b = 1;
        if (a != b) {
            //:: error: (assignment.type.incompatible)
            @LtLength("arr")
            int e = b;

        } else {

            @LtLength("arr")
            int c = b;
        }
        //:: error: (assignment.type.incompatible)
        @LtLength("arr")
        int d = b;
    }

    void testLTEL() {
        //:: error: (assignment.type.incompatible)
        @LteLength("arr")
        int a = Integer.parseInt("1");

        int b = 1;
        if (a != b) {
            //:: error: (assignment.type.incompatible)
            @LteLength("arr")
            int e = b;
        } else {
            @LteLength("arr")
            int c = b;

            //:: error: (assignment.type.incompatible)
            @LtLength("arr")
            int g = b;
        }
        //:: error: (assignment.type.incompatible)
        @LteLength("arr")
        int d = b;
    }
}
