import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

class EqualTo {
    static int[] a = {0};

    public static void equalToUpper(@LTLengthOf("a") int m, @LTEqLengthOf("a") int r) {
        if (r == m) {
            @LTLengthOf("a") int j = r;
        }
    }
}
