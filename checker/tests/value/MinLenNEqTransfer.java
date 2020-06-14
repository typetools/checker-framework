import org.checkerframework.common.value.qual.*;

class MinLenNEqTransfer {
    void neq_check(int[] a) {
        if (1 != a.length) {
            int x = 1; // do nothing.
        } else {
            int @MinLen(1) [] b = a;
        }
    }

    void neq_bad_check(int[] a) {
        if (1 != a.length) {
            int x = 1; // do nothing.
        } else {
            // :: error: (assignment.type.incompatible)
            int @MinLen(2) [] b = a;
        }
    }

    void neq_zero_special_case(int[] a) {
        if (a.length != 0) {
            int @MinLen(1) [] b = a;
        }
    }
}
