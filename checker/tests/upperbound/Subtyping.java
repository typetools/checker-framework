import org.checkerframework.checker.upperbound.qual.*;

public class Subtyping {
    void test(@LTEqLengthOf({"arr", "arr2", "arr3"}) int test) {
        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf({"arr"}) int a = 1;
        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"arr"}) int a1 = 1;

        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"arr"}) int b = a;
        @UpperBoundUnknown int d = a;

        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"arr2"}) int g = a;

        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf({"arr", "arr2", "arr3"}) int h = 2;

        @LTEqLengthOf({"arr", "arr2"}) int h2 = test;
        @LTEqLengthOf({"arr"}) int i = test;
        @LTEqLengthOf({"arr", "arr3"}) int j = test;
    }
}
