import org.checkerframework.checker.upperbound.qual.*;

class GreaterThanTransfer {
    void test() {
        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int a = Integer.parseInt("1");

        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int a3 = Integer.parseInt("3");

        int b = 2;
        if (a3 > b) {
            @LessThanLength("arr") int c = b;
        }
        //:: error: (assignment.type.incompatible)
        @LessThanLength("arr") int c1 = b;

        if (a > b) {
            int potato = 7;
        } else {
            //:: error: (assignment.type.incompatible)
            @LessThanLength("arr") int d = b;
        }
    }
}
