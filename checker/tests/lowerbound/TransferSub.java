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
        //:: error: (assignment.type.incompatible)
        @NonNegative int o = b - j;
        //:: error: (assignment.type.incompatible)
        @GTENegativeOne int p = i - d;

        // decrements

        //:: error: (assignment.type.incompatible)
        @Positive int q = --k;

        @Positive int r = k--;
        //:: error: (assignment.type.incompatible)
        @Positive int r1 = k;

        @NonNegative int s = k--;
        @NonNegative int t = --k;

        @GTENegativeOne int u = j--;
        @GTENegativeOne int v = --j;

        //:: error: (assignment.type.incompatible)
        @GTENegativeOne int w = --u;

        @GTENegativeOne int x = u--;
        //:: error: (assignment.type.incompatible)
        @GTENegativeOne int x1 = u;
    }
}
