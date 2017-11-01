import org.checkerframework.checker.index.qual.LTLengthOf;

@SuppressWarnings("lowerbound")
public class UpperBoundRefinement {
    // If expression i has type @LTLengthOf(value = "f2", offset = "f1.length") int and expression
    // j is less than or equal to the length of f1, then the type of i + j is @LTLengthOf("f2")
    void test(int[] f1, int[] f2) {
        @LTLengthOf(value = "f2", offset = "f1.length") int i = (f2.length - 1) - f1.length;
        @LTLengthOf("f1") int j = f1.length - 1;
        @LTLengthOf("f2") int x = i + j;
        @LTLengthOf("f2") int y = i + f1.length;
    }

    void test2() {
        double[] f1 = new double[10];
        double[] f2 = new double[20];

        for (int j = 0; j < f2.length; j++) {
            f2[j] = j;
        }
        for (int i = 0; i < f2.length - f1.length; i++) {
            // fill up f1 with elements of f2
            for (int j = 0; j < f1.length; j++) {
                f1[j] = f2[i + j];
            }
        }
    }

    public void test3(double[] a, double[] sub) {
        int a_index_max = a.length - sub.length;
        // Has type @LTL(value={"a","sub"}, offset={"-1 + sub.length", "-1 + a.length"})

        for (int i = 0; i <= a_index_max; i++) { // i has the same type as a_index_max
            for (int j = 0; j < sub.length; j++) { // j is @LTL("sub")
                // i + j is safe here. Because j is LTL("sub"), it should count as ("-1 +
                // sub.length")
                double d = a[i + j];
            }
        }
    }
}
