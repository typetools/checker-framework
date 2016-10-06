import org.checkerframework.checker.upperbound.qual.*;

class RefineEq {
    void testLTL() {
        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int a = Integer.parseInt("1");

        int b = 1;
        if (a == b) {
            @LessThanLength("arr") int c = b;

        } else {
            //:: error: (assignment.type.incompatible)
            @LessThanLength("arr") int e = b;
        }
        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int d = b;
    }

    void testLTEL() {
        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength("arr") int a = Integer.parseInt("1");

        int b = 1;
        if (a == b) {
            @LessThanOrEqualToLength("arr") int c = b;

            //:: error: (assignment.type.incompatible)
            @LessThanLength("arr") int g = b;
        } else {
            //:: error: (assignment.type.incompatible)
            @LessThanOrEqualToLength("arr") int e = b;
        }
        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength("arr") int d = b;
    }
}
