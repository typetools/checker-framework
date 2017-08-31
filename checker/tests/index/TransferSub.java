import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class TransferSub {

    void test() {
        // zero, one, and two
        int a = 1;

        @NonNegative int b = a - 1;
        //:: error: (assignment.type.incompatible)
        @Positive int c = a - 1;
        @GTENegativeOne int d = a - 2;

        //:: error: (assignment.type.incompatible)
        @NonNegative int e = a - 2;

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

        //:: error: (compound.assignment.type.incompatible) :: error: (assignment.type.incompatible)
        @Positive int q = --k; // k = 0

        //:: error: (compound.assignment.type.incompatible)
        @NonNegative int r = k--; // after this k = -1

        int k1 = 0;
        @NonNegative int s = k1--;

        //:: error: (assignment.type.incompatible)
        @NonNegative int s1 = k1;

        // transferred to SimpleTransferSub.java
        // this section is failing due to CF bug
        // int k2 = 0;
        // //:: error: (assignment.type.incompatible)
        // @Positive int s2 = k2--;

        k1 = 1;
        @NonNegative int t = --k1;

        k1 = 1;
        //:: error: (assignment.type.incompatible)
        @Positive int t1 = --k1;

        int u1 = -1;
        @GTENegativeOne int x = u1--;
        //:: error: (assignment.type.incompatible)
        @GTENegativeOne int x1 = u1;
    }
}
//a comment
