import org.checkerframework.common.value.qual.IntRange;

public class RefinementEq {

    void test_equal(int a, int j, int s) {

        if (-1 == a) {
            @IntRange(from = -1) int b = a;
        } else {
            // :: error: (assignment.type.incompatible)
            @IntRange(from = -1) int c = a;
        }

        if (0 == j) {
            @IntRange(from = 0) int k = j;
        } else {
            // :: error: (assignment.type.incompatible)
            @IntRange(from = 0) int l = j;
        }

        if (1 == s) {
            @IntRange(from = 1) int t = s;
        } else {
            // :: error: (assignment.type.incompatible)
            @IntRange(from = 1) int u = s;
        }
    }
}
