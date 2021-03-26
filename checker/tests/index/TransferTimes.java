import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class TransferTimes {

  void test() {
    int a = 1;
    @Positive int b = a * 1;
    @Positive int c = 1 * a;
    @NonNegative int d = 0 * a;
    // :: error: (assignment.type.incompatible)
    @NonNegative int e = -1 * a;

    int g = -1;
    @NonNegative int h = g * 0;
    // :: error: (assignment.type.incompatible)
    @Positive int i = g * 0;
    // :: error: (assignment.type.incompatible)
    @Positive int j = g * a;

    int k = 0;
    int l = 1;
    @Positive int m = a * l;
    @NonNegative int n = k * l;
    @NonNegative int o = k * k;
  }
}
// a comment
