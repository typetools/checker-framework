import org.checkerframework.common.value.qual.*;

public class MinLenGTTransfer {
    void gt_check(int[] a) {
        if (a.length > 0) {
            int @MinLen(1) [] b = a;
        }
    }

    void gt_bad_check(int[] a) {
        if (a.length > 0) {
            // :: error: (assignment.type.incompatible)
            int @MinLen(2) [] b = a;
        }
    }
}
