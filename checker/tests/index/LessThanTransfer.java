import org.checkerframework.checker.index.qual.MinLen;

class LessThanTransfer {
    void lt_check(int[] a) {
        if (0 < a.length) {
            int @MinLen(1) [] b = a;
        }
    }

    void lt_bad_check(int[] a) {
        if (0 < a.length) {
            //:: error: (assignment.type.incompatible)
            int @MinLen(2) [] b = a;
        }
    }
}
