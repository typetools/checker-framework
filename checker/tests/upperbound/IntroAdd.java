import org.checkerframework.checker.upperbound.qual.*;

public class IntroAdd {
    void test() {
        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"banana"}) int a = 3;
        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"banana"}) int c = a + 1;
        @LTEqLengthOf({"banana"}) int c1 = a + 1;
        @LTLengthOf({"banana"}) int d = a + 0;
        @LTLengthOf({"banana"}) int e = a + (-7);
        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"banana"}) int f = a + 7;
    }
}
