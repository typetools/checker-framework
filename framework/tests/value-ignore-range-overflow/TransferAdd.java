import org.checkerframework.common.value.qual.*;

public class TransferAdd {

  void test() {

    // adding zero and one and two

    int a = -1;

    @IntRange(from = 1) int a1 = a + 2;

    @IntRange(from = 0) int b = a + 1;
    @IntRange(from = 0) int c = 1 + a;

    @IntRange(from = -1) int d = a + 0;
    @IntRange(from = -1) int e = 0 + a;

    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int f = a + 1;

    @IntRange(from = 0) int g = b + 0;

    @IntRange(from = 1) int h = b + 1;

    @IntRange(from = 1) int i = h + 1;
    @IntRange(from = 1) int j = h + 0;

    // adding values

    @IntRange(from = 1) int k = i + j;
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int l = b + c;
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int m = d + c;
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int n = d + e;

    @IntRange(from = 1) int o = h + g;
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int p = h + d;

    @IntRange(from = 0) int q = b + c;
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 0) int r = q + d;

    @IntRange(from = 0) int s = k + d;
    @IntRange(from = -1) int t = s + d;

    // increments

    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int u = b++;

    @IntRange(from = 1) int u1 = b;

    @IntRange(from = 1) int v = ++c;

    @IntRange(from = 1) int v1 = c;

    int n1p1 = -1, n1p2 = -1;

    @IntRange(from = 0) int w = ++n1p1;

    @IntRange(from = 0) int w1 = n1p1;

    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int w2 = n1p1;
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int w3 = n1p1++;

    // :: error: (assignment.type.incompatible)
    @IntRange(from = 0) int x = n1p2++;

    @IntRange(from = 0) int x1 = n1p2;

    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int y = ++d;
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 1) int z = e++;
  }
}
