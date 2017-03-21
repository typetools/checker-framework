import org.checkerframework.checker.index.qual.*;

class Issue58Minimization {

    void test(@GTENegativeOne int x) {
        int z;
        if ((z = x) != -1) {
            @NonNegative int y = z;
        }
        if ((z = x) != 1) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int y = z;
        }
    }

    void test2(@NonNegative int x) {
        int z;
        if ((z = x) != 0) {
            @Positive int y = z;
        }
        if ((z = x) == 0) {
            // do nothing
            int y = 5;
        } else {
            @Positive int y = x;
        }
    }

    void ubc_test(int[] a, @LTEqLengthOf("#1") int x) {
        int z;
        if ((z = x) != a.length) {
            @LTLengthOf("a") int y = z;
        }
    }

    void minlen_test(int[] a, int[] c) {
        int[] b;
        if ((b = a) == c) {
            int @SameLen({"a", "b", "c"}) [] d = b;
        }
    }
}
