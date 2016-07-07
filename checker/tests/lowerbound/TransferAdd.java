import org.checkerframework.checker.lowerbound.qual.*;

public class TransferAdd {

    void test() {

        // adding zero and one and two

        int a = -1;

        @Positive int a1 = a + 2;

        @NonNegative int b = a + 1;
        @NonNegative int c = 1 + a;

        @NegativeOnePlus int d = a + 0;
        @NegativeOnePlus int e = 0 + a;

        //:: error: (assignment.type.incompatible)
        @Positive int f = a + 1;

        @NonNegative int g = b + 0;

        @Positive int h = b + 1;

        @Positive int i = h + 1;
        @Positive int j = h + 0;

        // adding values

        @Positive int k = i + j;
        //:: error: (assignment.type.incompatible)
        @Positive int l = b + c;
        //:: error: (assignment.type.incompatible)
        @Positive int m = d + c;
        //:: error: (assignment.type.incompatible)
        @Positive int n = d + e;

        @Positive int o = h + g;
        //:: error: (assignment.type.incompatible)
        @Positive int p = h + d;

        @NonNegative int q = b + c;
        //:: error: (assignment.type.incompatible)
        @NonNegative int r = q + d;

        @NonNegative int s = k + d;
        @NegativeOnePlus int t = s + d;

        // increments

        @Positive int u = b++;
        @Positive int v = ++c;

        int n1p1 = -1, n1p2 = -1;

        @NonNegative int w = ++n1p1;
        @NonNegative int x = n1p2++;

        //:: error: (assignment.type.incompatible)
        @Positive int y = ++d;
        //:: error: (assignment.type.incompatible)
        @Positive int z = e++;
    }
}
