import org.checkerframework.checker.index.qual.MinLen;

class GreaterThanOrEqualTransfer {
    void gte_check(int[] a) {
        if (a.length >= 1) {
            int @MinLen(1) [] b = a;
        }
    }

    void gte_bad_check(int[] a) {
        if (a.length >= 1) {
            //:: error: (assignment.type.incompatible)
            int @MinLen(2) [] b = a;
        }
    }
}
