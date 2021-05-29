import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class TransferAdd {

  void test() {

    // adding zero and one and two

    int a = -1;

    @Positive int a1 = a + 2;

    @NonNegative int b = a + 1;
    @NonNegative int c = 1 + a;

    @GTENegativeOne int d = a + 0;
    @GTENegativeOne int e = 0 + a;

    // :: error: (assignment)
    @Positive int f = a + 1;

    @NonNegative int g = b + 0;

    @Positive int h = b + 1;

    @Positive int i = h + 1;
    @Positive int j = h + 0;

    // adding values

    @Positive int k = i + j;
    // :: error: (assignment)
    @Positive int l = b + c;
    // :: error: (assignment)
    @Positive int m = d + c;
    // :: error: (assignment)
    @Positive int n = d + e;

    @Positive int o = h + g;
    // :: error: (assignment)
    @Positive int p = h + d;

    @NonNegative int q = b + c;
    // :: error: (assignment)
    @NonNegative int r = q + d;

    @NonNegative int s = k + d;
    @GTENegativeOne int t = s + d;

    // increments

    // :: error: (assignment)
    @Positive int u = b++;

    @Positive int u1 = b;

    @Positive int v = ++c;

    @Positive int v1 = c;

    int n1p1 = -1, n1p2 = -1;

    @NonNegative int w = ++n1p1;

    @NonNegative int w1 = n1p1;

    // :: error: (assignment)
    @Positive int w2 = n1p1;
    // :: error: (assignment)
    @Positive int w3 = n1p1++;

    // :: error: (assignment)
    @NonNegative int x = n1p2++;

    @NonNegative int x1 = n1p2;

    // :: error: (assignment)
    @Positive int y = ++d;
    // :: error: (assignment)
    @Positive int z = e++;
  }
}
// a comment
