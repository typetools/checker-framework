import org.checkerframework.common.value.qual.*;

public class TransferDivide {

  void test() {
    int a = -1;
    int b = 0;
    int c = 1;
    int d = 2;

    /** literals */
    @IntRange(from = 1) int e = -1 / -1;

    /** 0 / * -> NN */
    @IntRange(from = 0) int f = 0 / a;
    @IntRange(from = 0) int g = 0 / d;

    /** * / 1 -> * */
    @IntRange(from = -1) int h = a / 1;
    @IntRange(from = 0) int i = b / 1;
    @IntRange(from = 1) int j = c / 1;
    @IntRange(from = 1) int k = d / 1;

    /** pos / pos -> nn */
    @IntRange(from = 0) int l = d / c;
    @IntRange(from = 0) int m = c / d;
    // :: error: (assignment)
    @IntRange(from = 1) int n = c / d;

    /** nn / pos -> nn */
    @IntRange(from = 0) int o = b / c;
    // :: error: (assignment)
    @IntRange(from = 1) int p = b / d;

    /** pos / nn -> nn */
    @IntRange(from = 0) int q = d / l;
    // :: error: (assignment)
    @IntRange(from = 1) int r = c / l;

    /** nn / nn -> nn */
    @IntRange(from = 0) int s = b / q;
    // :: error: (assignment)
    @IntRange(from = 1) int t = b / q;

    /** n1p / pos -> n1p */
    @IntRange(from = -1) int u = a / d;
    @IntRange(from = -1) int v = a / c;
    // :: error: (assignment)
    @IntRange(from = 0) int w = a / c;

    /** n1p / nn -> n1p */
    @IntRange(from = -1) int x = a / l;
  }

  void testDivideByTwo(@IntRange(from = 0) int x) {
    @IntRange(from = 0) int y = x / 2;
  }
}
