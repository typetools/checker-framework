import org.checkerframework.common.value.qual.*;

public class MinLenLTTransfer {
    void lt_check(int[] a) {
        if (0 < a.length) {
            int @MinLen(1) [] b = a;
        }
    }

    void lt_bad_check(int[] a) {
        if (0 < a.length) {
            // :: error: (assignment.type.incompatible)
            int @MinLen(2) [] b = a;
        }
    }
}
