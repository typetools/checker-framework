import org.checkerframework.common.value.qual.*;

class GTETransferBug {
    void gte_bad_check(int[] a) {
        if (a.length >= 1) {
            //:: error: (assignment.type.incompatible)
            int @ArrayLenRange(from = 2) [] b = a;

            int @ArrayLenRange(from = 1) [] c = a;
        }
    }
}
