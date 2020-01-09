import org.checkerframework.checker.index.qual.*;

class IntroAnd {
    void test() {
        @NonNegative int a = 1 & 0;
        @NonNegative int b = a & 5;

        // :: error: (assignment.type.incompatible)
        @Positive int c = a & b;
        @NonNegative int d = a & b;
        @NonNegative int e = b & a;
    }

    void test_ubc_and(
            @IndexFor("#2") int i, int[] a, @LTLengthOf("#2") int j, int k, @NonNegative int m) {
        int x = a[i & k];
        int x1 = a[k & i];
        // :: error: (array.access.unsafe.low) :: error: (array.access.unsafe.high)
        int y = a[j & k];
        if (j > -1) {
            int z = a[j & k];
        }
        // :: error: (array.access.unsafe.high)
        int w = a[m & k];
        if (m < a.length) {
            int u = a[m & k];
        }
    }

    void two_arrays(int[] a, int[] b, @IndexFor("#1") int i, @IndexFor("#2") int j) {
        int l = a[i & j];
        l = b[i & j];
    }

    void test_pos(@Positive int x, @Positive int y) {
        // :: error: (assignment.type.incompatible)
        @Positive int z = x & y;
    }
}
