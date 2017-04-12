import org.checkerframework.common.value.qual.*;

class ValueNoOverflow {
    void test_plus(@IntRange(from = 0) int x, @IntRange(from = -1) int z) {
        @IntRange(from = 1)
        int y = x + 1; // NonNegative to Positive
        @IntRange(from = 0)
        int w = z + 1; // GTEN1 to NN
    }

    void test_minus(@IntRange(to = 0) int x, @IntRange(to = 1) int z) {
        @IntRange(to = -1)
        int y = x - 1; // NonNegative to GTEN1
        @IntRange(to = 0)
        int w = z - 1; // Pos to NN
    }

    void test_mult(@IntRange(from = 0) int x, @IntRange(from = 1) int z) {
        @IntRange(from = 0)
        int y = x * z;
        @IntRange(from = 1)
        int w = z * z;
    }

    void testLong(@IntRange(from = 0) long x, @IntRange(from = 1) long z) {
        @IntRange(from = 0)
        long y = x * z;
        @IntRange(from = 1)
        long w = z * z;
    }
    // Include ArrayLenRange tests once ArrayLenRange is merged.
    /*
     void arraylenrange_test(int @ArrayLenRange(from = 5) [] a) {
         int @ArrayLenRange(from = 7) [] b = new int[a.length + 2];
     }
    */
}
