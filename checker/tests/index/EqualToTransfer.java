import org.checkerframework.common.value.qual.MinLen;

class EqualToTransfer {
    void eq_check(int[] a) {
        if (1 == a.length) {
            int @MinLen(1) [] b = a;
        }
        if (a.length == 1) {
            int @MinLen(1) [] b = a;
        }
    }

    void eq_bad_check(int[] a) {
        if (1 == a.length) {
            // :: error: (assignment.type.incompatible)
            int @MinLen(2) [] b = a;
        }
    }
}
