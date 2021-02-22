import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.common.value.qual.MinLen;

public class Issue58Minimization {

    void test(@GTENegativeOne int x) {
        int z;
        if ((z = x) != -1) {
            @NonNegative int y = z;
        }
        if ((z = x) != 1) {
            // :: error: (assignment.type.incompatible)
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

    void samelen_test(int[] a, int[] c) {
        int[] b;
        if ((b = a) == c) {
            int @SameLen({"a", "b", "c"}) [] d = b;
        }
    }

    void minlen_test(int[] a, int @MinLen(1) [] c) {
        int[] b;
        if ((b = a) == c) {
            int @MinLen(1) [] d = b;
        }
    }

    void minlen_test2(int[] a, int x) {
        int one = 1;
        if ((x = one) == a.length) {
            int @MinLen(1) [] b = a;
        }
    }
}
