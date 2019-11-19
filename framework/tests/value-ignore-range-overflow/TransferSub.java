import org.checkerframework.common.value.qual.*;

public class TransferSub {

    void test() {
        // zero, one, and two
        int a = 1;

        @IntRange(from = 0) int b = a - 1;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 1) int c = a - 1;
        @IntRange(from = -1) int d = a - 2;

        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0) int e = a - 2;

        @IntRange(from = -1) int f = b - 1;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0) int g = b - 1;

        // :: error: (assignment.type.incompatible)
        @IntRange(from = -1) int h = f - 1;

        @IntRange(from = -1) int i = f - 0;
        @IntRange(from = 0) int j = b - 0;
        @IntRange(from = 1) int k = a - 0;

        // :: error: (assignment.type.incompatible)
        @IntRange(from = 1) int l = j - 0;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0) int m = i - 0;

        // :: error: (assignment.type.incompatible)
        @IntRange(from = 1) int n = a - k;
        // this would be an error if the values of b and j (both zero) weren't known at compile time
        @IntRange(from = 0) int o = b - j;
        /* i and d both have compile time value -1, so this is legal.
        The general case of GTEN1 - GTEN1 is not, though. */
        @IntRange(from = -1) int p = i - d;

        // decrements

        // :: error: (unary.decrement.type.incompatible)
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 1) int q = --k; // k = 0

        // :: error: (unary.decrement.type.incompatible)
        @IntRange(from = 0) int r = k--; // after this k = -1

        int k1 = 0;
        @IntRange(from = 0) int s = k1--;

        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0) int s1 = k1;

        k1 = 1;
        @IntRange(from = 0) int t = --k1;

        k1 = 1;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 1) int t1 = --k1;

        int u1 = -1;
        @IntRange(from = -1) int x = u1--;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = -1) int x1 = u1;
    }
}
