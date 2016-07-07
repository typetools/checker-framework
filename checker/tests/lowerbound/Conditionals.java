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
}
