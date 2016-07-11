import org.checkerframework.checker.lowerbound.qual.*;

public class RefinementEq {

    void test_equal() {

        int a = Integer.parseInt("0"); /** 0 */
        //:: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (-1 == a) {
            @GTENegativeOne int b = a;
        } else {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int c = a;
        }

        int d = Integer.parseInt("-2"); /** -2 */
        if (-1 == d) {
            @GTENegativeOne int e = d;
        } else {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int f = d;
        }

        int g = Integer.parseInt("5"); /** 5 */
        if (-1 == g) {
            @GTENegativeOne int h = g;
        } else {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int i = g;
        }

        int j = Integer.parseInt("0"); /** 0 */
        if (0 == j) {
            @NonNegative int k = j;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int l = j;
        }

        int m = Integer.parseInt("-2"); /** -2 */
        if (0 == m) {
            @NonNegative int n = m;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int o = m;
        }

        int p = Integer.parseInt("5"); /** 5 */
        if (0 == p) {
            @NonNegative int q = p;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int r = p;
        }

        int s = Integer.parseInt("0"); /** 0 */
        if (1 == s) {
            @Positive int t = s;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int u = s;
        }

        int v = Integer.parseInt("-2"); /** -2 */
        if (1 == v) {
            @Positive int w = v;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int x = v;
        }

        int y = Integer.parseInt("5"); /** 5 */
        if (1 == y) {
            @Positive int z = y;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int ab = y;
        }

    }
}
