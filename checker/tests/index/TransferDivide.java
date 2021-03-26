import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class TransferDivide {

  void test() {
    int a = -1;
    int b = 0;
    int c = 1;
    int d = 2;

    /** literals */
    @Positive int e = -1 / -1;

    /** 0 / * -> NN */
    @NonNegative int f = 0 / a;
    @NonNegative int g = 0 / d;

    /** * / 1 -> * */
    @GTENegativeOne int h = a / 1;
    @NonNegative int i = b / 1;
    @Positive int j = c / 1;
    @Positive int k = d / 1;

    /** pos / pos -> nn */
    @NonNegative int l = d / c;
    @NonNegative int m = c / d;
    // :: error: (assignment.type.incompatible)
    @Positive int n = c / d;

    /** nn / pos -> nn */
    @NonNegative int o = b / c;
    // :: error: (assignment.type.incompatible)
    @Positive int p = b / d;

    /** pos / nn -> nn */
    @NonNegative int q = d / l;
    // :: error: (assignment.type.incompatible)
    @Positive int r = c / l;

    /** nn / nn -> nn */
    @NonNegative int s = b / q;
    // :: error: (assignment.type.incompatible)
    @Positive int t = b / q;

    /** n1p / pos -> n1p */
    @GTENegativeOne int u = a / d;
    @GTENegativeOne int v = a / c;
    // :: error: (assignment.type.incompatible)
    @NonNegative int w = a / c;

    /** n1p / nn -> n1p */
    @GTENegativeOne int x = a / l;
  }

  void testDivideByTwo(@NonNegative int x) {
    @NonNegative int y = x / 2;
  }
}
// a comment
