import org.checkerframework.checker.lowerbound.qual.*;

public class TransferSub {

    void test() {
        // zero, one, and two
        int a = 1;

        @NonNegative int b = a - 1;
        //:: error: (assignment.type.incompatible)
        @Positive int c = a - 1;
        @GTENegativeOne int d = a - 2;

        //:: error: (assignment.type.incompatible)
        @NonNegative int e = a -2;

        @GTENegativeOne int f = b - 1;
        //:: error: (assignment.type.incompatible)
        @NonNegative int g = b - 1;

        //:: error: (assignment.type.incompatible)
        @GTENegativeOne int h = f - 1;

        @GTENegativeOne int i = f - 0;
        @NonNegative int j = b - 0;
        @Positive int k = a - 0;

        //:: error: (assignment.type.incompatible)
        @Positive int l = j - 0;
        //:: error: (assignment.type.incompatible)
        @NonNegative int m = i - 0;

        //:: error: (assignment.type.incompatible)
        @Positive int n = a - k;
        // this would be an error if the values of b and j (both zero) weren't known at compile time
        @NonNegative int o = b - j;
        /* i and d both have compile time value -1, so this is legal.
           The general case of GTEN1 - GTEN1 is not, though. */
        @GTENegativeOne int p = i - d;

        // decrements

        //:: error: (assignment.type.incompatible)
        @Positive int q = --k;

        @Positive int r = k--;
        // this should be:: error: (assignment.type.incompatible)
        // but isn't because inc and dec don't actually work correctly yet
        @Positive int r1 = k;

        @NonNegative int s = k--;
        @NonNegative int t = --k;

        @GTENegativeOne int u = j--;
        @GTENegativeOne int v = --j;

        //:: error: (assignment.type.incompatible)
        @GTENegativeOne int w = --u;

        @GTENegativeOne int x = u--;
        // this should be:: error: (assignment.type.incompatible)
        // but isn't because inc and dec don't actually work correctly yet
        @GTENegativeOne int x1 = u;
    }
}
