import org.checkerframework.checker.lowerbound.qual.*;

public class Conditionals {
    public void test() {
        /** these tests are meant to check whether the refinement rules
            for conditional expressions work as intended */

        /** in the definitions that follow, I use the ~ to mean "is an element of"

        /** greater than:
            x > N1P -> x ~ NN
            x > NN -> x ~ POS
            x > POS -> x ~ POS
        */

        int a = Integer.parseInt("0"); /** 0 */
        //:: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (a > -1) {
            /** a is NN now */
            @NonNegative int b = a;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int c = a;
        }

        int d = Integer.parseInt("-2"); /** -2 */
        if (d > -1) {
            /** a is NN now */
            @NonNegative int e = d;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int f = d;
        }

        int g = Integer.parseInt("5"); /** 5 */
        if (g > -1) {
            /** a is NN now */
            @NonNegative int h = g;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int i = g;
        }

        int j = Integer.parseInt("0"); /** 0 */
        if (j > 0) {
            /** a is POS now */
            @Positive int k = j;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int l = j;
        }

        int m = Integer.parseInt("-2"); /** -2 */
        if (m > 0) {
            /** a is POS now */
            @Positive int n = m;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int o = m;
        }

        int p = Integer.parseInt("5"); /** 5 */
        if (p > 0) {
            /** a is POS now */
            @Positive int q = p;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int r = p;
        }
    }

    void test2() {
        /** greater than or equal:
            x >= N1P -> x ~ N1P
            x >= NN -> x ~ NN
            x >= POS -> x ~ POS
        */

        int a = Integer.parseInt("0"); /** 0 */
        //:: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (a >= -1) {
            @NegativeOnePlus int b = a;
        } else {
            //:: error: (assignment.type.incompatible)
            @NegativeOnePlus int c = a;
        }

        int d = Integer.parseInt("-2"); /** -2 */
        if (d >= -1) {
            @NegativeOnePlus int e = d;
        } else {
            //:: error: (assignment.type.incompatible)
            @NegativeOnePlus int f = d;
        }

        int g = Integer.parseInt("5"); /** 5 */
        if (g >= -1) {
            @NegativeOnePlus int h = g;
        } else {
            //:: error: (assignment.type.incompatible)
            @NegativeOnePlus int i = g;
        }

        int j = Integer.parseInt("0"); /** 0 */
        if (j >= 0) {
            @NonNegative int k = j;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int l = j;
        }

        int m = Integer.parseInt("-2"); /** -2 */
        if (m >= 0) {
            @NonNegative int n = m;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int o = m;
        }

        int p = Integer.parseInt("5"); /** 5 */
        if (p >= 0) {
            @NonNegative int q = p;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int r = p;
        }

        int s = Integer.parseInt("0"); /** 0 */
        if (s >= 1) {
            @Positive int t = s;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int u = s;
        }

        int v = Integer.parseInt("-2"); /** -2 */
        if (v >= 1) {
            @Positive int w = v;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int x = v;
        }

        int y = Integer.parseInt("5"); /** 5 */
        if (y >= 1) {
            @Positive int z = y;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int ab = y;
        }
    }

    void test3() {
        /** less than or equal:
            N1P <= x -> x ~ N1P
            NN <= x -> x ~ NN
            POS <=  -> x ~ POS
        */

        int a = Integer.parseInt("0"); /** 0 */
        //:: error: (assignment.type.incompatible)
        @NonNegative int aa = a;
        if (-1 <= a) {
            @NegativeOnePlus int b = a;
        } else {
            //:: error: (assignment.type.incompatible)
            @NegativeOnePlus int c = a;
        }

        int d = Integer.parseInt("-2"); /** -2 */
        if (-1 <= d) {
            @NegativeOnePlus int e = d;
        } else {
            //:: error: (assignment.type.incompatible)
            @NegativeOnePlus int f = d;
        }

        int g = Integer.parseInt("5"); /** 5 */
        if (-1 <= g) {
            @NegativeOnePlus int h = g;
        } else {
            //:: error: (assignment.type.incompatible)
            @NegativeOnePlus int i = g;
        }

        int j = Integer.parseInt("0"); /** 0 */
        if (0 <= j) {
            @NonNegative int k = j;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int l = j;
        }

        int m = Integer.parseInt("-2"); /** -2 */
        if (0 <= m) {
            @NonNegative int n = m;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int o = m;
        }

        int p = Integer.parseInt("5"); /** 5 */
        if (0 <= p) {
            @NonNegative int q = p;
        } else {
            //:: error: (assignment.type.incompatible)
            @NonNegative int r = p;
        }

        int s = Integer.parseInt("0"); /** 0 */
        if (1 <= s) {
            @Positive int t = s;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int u = s;
        }

        int v = Integer.parseInt("-2"); /** -2 */
        if (1 <= v) {
            @Positive int w = v;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int x = v;
        }

        int y = Integer.parseInt("5"); /** 5 */
        if (1 <= y) {
            @Positive int z = y;
        } else {
            //:: error: (assignment.type.incompatible)
            @Positive int ab = y;
        }
    }

}
