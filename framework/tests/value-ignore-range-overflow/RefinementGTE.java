import org.checkerframework.common.value.qual.IntRange;

public class RefinementGTE {

    void test_forward(int a, int j, int s) {
        /** forwards greater than or equals */
        //:: error: (assignment.type.incompatible)
        @IntRange(from = -1) int aa = a;
        if (a >= -1) {
            @IntRange(from = -1) int b = a;
        } else {
            //:: error: (assignment.type.incompatible)
            @IntRange(from = -1) int c = a;
        }

        if (j >= 0) {
            @IntRange(from = 0) int k = j;
            //:: error: (assignment.type.incompatible)
            @IntRange(from = 1) int k1 = j;
            @IntRange(from = -1) int k2 = j;
        } else {
            //:: error: (assignment.type.incompatible)
            @IntRange(from = 0) int l = j;
        }
    }

    void test_backwards(int a, int j, int s) {
        /** backwards greater than or equal */
        //:: error: (assignment.type.incompatible)
        @IntRange(from = 0) int aa = a;
        if (-1 >= a) {
            //:: error: (assignment.type.incompatible)
            @IntRange(from = 0) int b = a;
        } else {
            @IntRange(from = 0) int c = a;
        }

        if (0 >= j) {
            //:: error: (assignment.type.incompatible)
            @IntRange(from = 1) int k = j;
        } else {
            @IntRange(from = 1) int l = j;
            @IntRange(from = -1) int l1 = j;
            @IntRange(from = 0) int l2 = j;
        }

        if (1 >= s) {
            //:: error: (assignment.type.incompatible)
            @IntRange(from = 1) int t = s;
        } else {
            @IntRange(from = 1) int u = s;
        }
    }
}
