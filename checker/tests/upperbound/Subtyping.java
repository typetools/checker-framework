import org.checkerframework.checker.upperbound.qual.*;

public class Subtyping {
    void test() {
        //:: error: (assignment.type.incompatible)
        @LteLength({"arr"})
        int a = 1;
        //:: error: (assignment.type.incompatible)
        @LtLength({"arr"})
        int a1 = 1;

        //:: error: (assignment.type.incompatible)
        @LtLength({"arr"})
        int b = a;
        @UpperBoundUnknown int d = a;

        //:: error: (assignment.type.incompatible)
        @LtLength({"arr2"})
        int g = a;

        //:: error: (assignment.type.incompatible)
        @LteLength({"arr", "arr2", "arr3"})
        int h = 2;

        @LteLength({"arr", "arr2"})
        int h2 = h;
        @LteLength({"arr"})
        int i = h;
        @LteLength({"arr", "arr3"})
        int j = h;
    }
}
