import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class RefinementLTE {

    void test_backwards(int a, int j, int s) {
        /** backwards less than or equals */
        // :: error: (assignment.type.incompatible)
        @GTENegativeOne int aa = a;
        if (-1 <= a) {
            @GTENegativeOne int b = a;
        } else {
            // :: error: (assignment.type.incompatible)
            @GTENegativeOne int c = a;
        }

        if (0 <= j) {
            @NonNegative int k = j;
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNegative int l = j;
        }

        if (1 <= s) {
            @Positive int t = s;
        } else {
            // :: error: (assignment.type.incompatible)
            @Positive int u = s;
        }
    }

    void test_forwards(int a, int j, int s) {
        /** forwards less than or equal */
        // :: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (a <= -1) {
            // :: error: (assignment.type.incompatible)
            @NonNegative int b = a;
        } else {
            @NonNegative int c = a;
        }

        if (j <= 0) {
            // :: error: (assignment.type.incompatible)
            @Positive int k = j;
        } else {
            @Positive int l = j;
        }

        if (s <= 1) {
            // :: error: (assignment.type.incompatible)
            @Positive int t = s;
        } else {
            @Positive int u = s;
        }
    }
}
// a comment
