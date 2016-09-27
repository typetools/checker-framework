import org.checkerframework.checker.lowerbound.qual.*;

public class RefinementLT {

    void test_backwards() {
        /** backwards less than */
        int a = Integer.parseInt("0");
        /** 0 */
        //:: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (-1 < a) {
            @NonNegative int b = a;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int c = a;
        }

        int d = Integer.parseInt("-2");
        /** -2 */
        if (-1 < d) {
            @NonNegative int e = d;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int f = d;
        }

        int g = Integer.parseInt("5");
        /** 5 */
        if (-1 < g) {
            @NonNegative int h = g;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int i = g;
        }

        int j = Integer.parseInt("0");
        /** 0 */
        if (0 < j) {
            @Positive int k = j;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int l = j;
        }

        int m = Integer.parseInt("-2");
        /** -2 */
        if (0 < m) {
            @Positive int n = m;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int o = m;
        }

        int p = Integer.parseInt("5");
        /** 5 */
        if (0 < p) {
            @Positive int q = p;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int r = p;
        }

        int s = Integer.parseInt("0");
        /** 0 */
        if (1 < s) {
            @Positive int t = s;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int u = s;
        }

        int v = Integer.parseInt("-2");
        /** -2 */
        if (1 < v) {
            @Positive int w = v;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int x = v;
        }

        int y = Integer.parseInt("5");
        /** 5 */
        if (1 < y) {
            @Positive int z = y;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int ab = y;
        }
    }

    void test_forwards() {
        /** forwards less than */
        int a = Integer.parseInt("0");
        /** 0 */
        //:: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (a < -1) {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int b = a;
        } else {
            @GTENegativeOne int c = a;
        }

        int d = Integer.parseInt("-2");
        /** -2 */
        if (d < -1) {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int e = d;
        } else {
            @GTENegativeOne int f = d;
        }

        int g = Integer.parseInt("5");
        /** 5 */
        if (g < -1) {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int h = g;
        } else {
            @GTENegativeOne int i = g;
        }

        int j = Integer.parseInt("0");
        /** 0 */
        if (j < 0) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int k = j;
        } else {
            @NonNegative int l = j;
        }

        int m = Integer.parseInt("-2");
        /** -2 */
        if (m < 0) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int n = m;
        } else {
            @NonNegative int o = m;
        }

        int p = Integer.parseInt("5");
        /** 5 */
        if (p < 0) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int q = p;
        } else {
            @NonNegative int r = p;
        }

        int s = Integer.parseInt("0");
        /** 0 */
        if (s < 1) {
            //:: error: (assignment.type.incompatible)
            @Positive int t = s;
        } else {
            @Positive int u = s;
        }

        int v = Integer.parseInt("-2");
        /** -2 */
        if (v < 1) {
            //:: error: (assignment.type.incompatible)
            @Positive int w = v;
        } else {
            @Positive int x = v;
        }

        int y = Integer.parseInt("5");
        /** 5 */
        if (y < 1) {
            //:: error: (assignment.type.incompatible)
            @Positive int z = y;
        } else {
            @Positive int ab = y;
        }
    }
}
//a comment
