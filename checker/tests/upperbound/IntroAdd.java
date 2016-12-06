import org.checkerframework.checker.upperbound.qual.*;

public class IntroAdd {
    void test(/*@LTLengthOf({"banana"})*/ int test) {
        //:: error: (assignment.type.incompatible)
        /*@LTLengthOf({"banana"})*/ int a = 3;
        //:: error: (assignment.type.incompatible)
        /*@LTLengthOf({"banana"})*/ int c = test + 1;
        /*@LTEqLengthOf({"banana"})*/ int c1 = test + 1;
        /*@LTLengthOf({"banana"})*/ int d = test + 0;
        /*@LTLengthOf({"banana"})*/ int e = test + (-7);
        //:: error: (assignment.type.incompatible)
        /*@LTLengthOf({"banana"})*/ int f = test + 7;
    }
}
