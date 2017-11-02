import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class RefinementGT {

    void test_forward(int a, int j, int s) {
        /** forwards greater than */
        // :: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (a > -1) {
            /** a is NN now */
            @NonNegative int b = a;
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNegative int c = a;
        }

        if (j > 0) {
            /** j is POS now */
            @Positive int k = j;
        } else {
            // :: error: (assignment.type.incompatible)
            @Positive int l = j;
        }

        if (s > 1) {
            @Positive int t = s;
        } else {
            // :: error: (assignment.type.incompatible)
            @Positive int u = s;
        }
    }

    void test_backwards(int a, int j, int s) {
        /** backwards greater than */
        // :: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (-1 > a) {
            // :: error: (assignment.type.incompatible)
            @GTENegativeOne int b = a;
        } else {
            @GTENegativeOne int c = a;
        }

        if (0 > j) {
            // :: error: (assignment.type.incompatible)
            @NonNegative int k = j;
        } else {
            @NonNegative int l = j;
        }

        if (1 > s) {
            // :: error: (assignment.type.incompatible)
            @Positive int t = s;
        } else {
            @Positive int u = s;
        }
    }
}
// a comment
