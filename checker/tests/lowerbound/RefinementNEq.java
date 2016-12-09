import org.checkerframework.checker.lowerbound.qual.*;

public class RefinementNEq {

    void test_not_equal() {

        int a = Integer.parseInt("0");
        /** 0 */
        //:: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (-1 != a) {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int b = a;
        } else {
            @GTENegativeOne int c = a;
        }

        int d = Integer.parseInt("-2");
        /** -2 */
        if (-1 != d) {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int e = d;
        } else {
            @GTENegativeOne int f = d;
        }

        int g = Integer.parseInt("5");
        /** 5 */
        if (-1 != g) {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int h = g;
        } else {
            @GTENegativeOne int i = g;
        }

        int j = Integer.parseInt("0");
        /** 0 */
        if (0 != j) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int k = j;
        } else {
            @NonNegative int l = j;
        }

        int m = Integer.parseInt("-2");
        /** -2 */
        if (0 != m) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int n = m;
        } else {
            @NonNegative int o = m;
        }

        int p = Integer.parseInt("5");
        /** 5 */
        if (0 != p) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int q = p;
        } else {
            @NonNegative int r = p;
        }

        int s = Integer.parseInt("0");
        /** 0 */
        if (1 != s) {
            //:: error: (assignment.type.incompatible)
            @Positive int t = s;
        } else {
            @Positive int u = s;
        }

        int v = Integer.parseInt("-2");
        /** -2 */
        if (1 != v) {
            //:: error: (assignment.type.incompatible)
            @Positive int w = v;
        } else {
            @Positive int x = v;
        }

        int y = Integer.parseInt("5");
        /** 5 */
        if (1 != y) {
            //:: error: (assignment.type.incompatible)
            @Positive int z = y;
        } else {
            @Positive int ab = y;
        }
    }
}
//a comment
