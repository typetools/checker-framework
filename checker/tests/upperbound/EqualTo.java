import org.checkerframework.checker.upperbound.qual.*;

class EqualTo {
    static int[] a = {0};

    public static void equalToUpper(@LTLengthOf("a") int m, @LTEqLengthOf("a") int r) {
        if (r == m) {
            @LTLengthOf("a") int j = r;
        }
    }
}
