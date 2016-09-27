import org.checkerframework.checker.upperbound.qual.*;

class RefineGTE {
    void testLTL() {
        // The reason for the parsing is so that the Value Checker
        // can't figure it out but normal humans can.

        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int a = Integer.parseInt("1");

        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int a3 = Integer.parseInt("3");

        int b = 2;
        if (a3 >= b) {
            @LessThanLength("arr") int c = b;
        }
        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int c1 = b;

        if (a >= b) {
            int potato = 7;
        } else {
            //:: error: (assignment.type.incompatible)
            @LessThanLength("arr") int d = b;
        }
    }

    void testEL() {
        //:: error: (assignment.type.incompatible)
        @EqualToLength("arr") int a = Integer.parseInt("1");

        //:: error: (assignment.type.incompatible)
        @EqualToLength("arr") int a3 = Integer.parseInt("3");

        int b = 2;
        if (a3 >= b) {
            @LessThanOrEqualToLength("arr") int c = b;
        }
        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength("arr") int c1 = b;

        if (a >= b) {
            int potato = 7;
        } else {
            //:: error: (assignment.type.incompatible)
            @LessThanOrEqualToLength("arr") int d = b;
        }
    }

    void testLTEL() {
        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength("arr") int a = Integer.parseInt("1");

        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength("arr") int a3 = Integer.parseInt("3");

        int b = 2;
        if (a3 >= b) {
            @LessThanOrEqualToLength("arr") int c = b;
        }
        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength("arr") int c1 = b;

        if (a >= b) {
            int potato = 7;
        } else {
            //:: error: (assignment.type.incompatible)
            @LessThanOrEqualToLength("arr") int d = b;
        }
    }
}
