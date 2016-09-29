import org.checkerframework.checker.lowerbound.qual.*;

public class RefinementGTE {

    void test_forward() {
        /** forwards greater than or equals */
        int a = Integer.parseInt("0");
        /** 0 */
        //:: error: (assignment.type.incompatible)
        @GTENegativeOne int aa = a;
        if (a >= -1) {
            @GTENegativeOne int b = a;
        } else {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int c = a;
        }

        int d = Integer.parseInt("-2");
        /** -2 */
        if (d >= -1) {
            @GTENegativeOne int e = d;
        } else {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int f = d;
        }

        int g = Integer.parseInt("5");
        /** 5 */
        if (g >= -1) {
            @GTENegativeOne int h = g;
        } else {
            //:: error: (assignment.type.incompatible)
            @GTENegativeOne int i = g;
        }

        int j = Integer.parseInt("0");
        /** 0 */
        if (j >= 0) {
            @NonNegative int k = j;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int l = j;
        }

        int m = Integer.parseInt("-2");
        /** -2 */
        if (m >= 0) {
            @NonNegative int n = m;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int o = m;
        }

        int p = Integer.parseInt("5");
        /** 5 */
        if (p >= 0) {
            @NonNegative int q = p;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int r = p;
        }

        int s = Integer.parseInt("0");
        /** 0 */
        if (s >= 1) {
            @Positive int t = s;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int u = s;
        }

        int v = Integer.parseInt("-2");
        /** -2 */
        if (v >= 1) {
            @Positive int w = v;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int x = v;
        }

        int y = Integer.parseInt("5");
        /** 5 */
        if (y >= 1) {
            @Positive int z = y;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int ab = y;
        }
    }

    void test_backwards() {
        /** backwards greater than or equal */
        int a = Integer.parseInt("0");
        /** 0 */
        //:: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (-1 >= a) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int b = a;
        } else {
            @NonNegative int c = a;
        }

        int d = Integer.parseInt("-2");
        /** -2 */
        if (-1 >= d) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int e = d;
        } else {
            @NonNegative int f = d;
        }

        int g = Integer.parseInt("5");
        /** 5 */
        if (-1 >= g) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int h = g;
        } else {
            @NonNegative int i = g;
        }

        int j = Integer.parseInt("0");
        /** 0 */
        if (0 >= j) {
            //:: error: (assignment.type.incompatible)
            @Positive int k = j;
        } else {
            @Positive int l = j;
        }

        int m = Integer.parseInt("-2");
        /** -2 */
        if (0 >= m) {
            //:: error: (assignment.type.incompatible)
            @Positive int n = m;
        } else {
            @Positive int o = m;
        }

        int p = Integer.parseInt("5");
        /** 5 */
        if (0 >= p) {
            //:: error: (assignment.type.incompatible)
            @Positive int q = p;
        } else {
            @Positive int r = p;
        }

        int s = Integer.parseInt("0");
        /** 0 */
        if (1 >= s) {
            //:: error: (assignment.type.incompatible)
            @Positive int t = s;
        } else {
            @Positive int u = s;
        }

        int v = Integer.parseInt("-2");
        /** -2 */
        if (1 >= v) {
            //:: error: (assignment.type.incompatible)
            @Positive int w = v;
        } else {
            @Positive int x = v;
        }

        int y = Integer.parseInt("5");
        /** 5 */
        if (1 >= y) {
            //:: error: (assignment.type.incompatible)
            @Positive int z = y;
        } else {
            @Positive int ab = y;
        }
    }
}
//a comment
