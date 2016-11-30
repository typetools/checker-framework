import org.checkerframework.checker.upperbound.qual.*;

public class IntroSub {
    void test() {
        //:: error: (assignment.type.incompatible)
        /*@LtLength({"banana"})*/ int a = 3;
        //:: error: (assignment.type.incompatible)
        /*@LtLength({"banana"})*/ int c = a - (-1);
        /*@LteLength({"banana"})*/ int c1 = a - (-1);
        /*@LtLength({"banana"})*/ int d = a - 0;
        /*@LtLength({"banana"})*/ int e = a - 7;
        //:: error: (assignment.type.incompatible)
        /*@LtLength({"banana"})*/ int f = a - (-7);

        //:: error: (assignment.type.incompatible)
        /*@LteLength({"banana"})*/ int j = 7;
    }
}
