import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

class EqualTo {
    static final int[] a = {0};

    public static @LTLengthOf("a") int equalToUpper(
            @LTLengthOf("a") int m, @LTEqLengthOf("a") int r) {
        if (r == m) {
            return r;
        }
        return m;
    }
}
