import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class IntroSub {
    void test(int[] arr) {
        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"arr"}) int a = 3;
        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"arr"}) int c = a - (-1);
        @LTEqLengthOf({"arr"}) int c1 = a - (-1);
        @LTLengthOf({"arr"}) int d = a - 0;
        @LTLengthOf({"arr"}) int e = a - 7;
        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"arr"}) int f = a - (-7);

        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf({"arr"}) int j = 7;
    }
}
