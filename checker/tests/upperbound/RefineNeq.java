import org.checkerframework.checker.upperbound.qual.*;

class RefineNeq {
    void testLTL() {
        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int a = Integer.parseInt("1");

        int b = 1;
        if (a != b) {
            //:: error: (assignment.type.incompatible)
            @LessThanLength("arr") int e = b;

        } else {

            @LessThanLength("arr") int c = b;
        }
        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int d = b;
    }

    void testLTEL() {
        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength("arr") int a = Integer.parseInt("1");

        int b = 1;
        if (a != b) {
            //:: error: (assignment.type.incompatible)
            @LessThanOrEqualToLength("arr") int e = b;
        } else {
            @LessThanOrEqualToLength("arr") int c = b;

            //:: error: (assignment.type.incompatible)
            @LessThanLength("arr") int g = b;
        }
        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength("arr") int d = b;
    }
}
