import org.checkerframework.common.value.qual.IntRange;

public class RefinementNEq {

    void test_not_equal(int a, int j, int s) {

        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0) int aa = a;
        if (-1 != a) {
            // :: error: (assignment.type.incompatible)
            @IntRange(from = -1) int b = a;
        } else {
            @IntRange(from = -1) int c = a;
        }

        if (0 != j) {
            // :: error: (assignment.type.incompatible)
            @IntRange(from = 0) int k = j;
        } else {
            @IntRange(from = 0) int l = j;
        }

        if (1 != s) {
            // :: error: (assignment.type.incompatible)
            @IntRange(from = 1) int t = s;
        } else {
            @IntRange(from = 1) int u = s;
        }
    }
}
