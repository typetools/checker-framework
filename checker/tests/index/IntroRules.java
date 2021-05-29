import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.LowerBoundUnknown;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class IntroRules {

  void test() {
    @Positive int a = 10;
    @NonNegative int b = 9;
    @GTENegativeOne int c = 8;
    @LowerBoundUnknown int d = 7;

    // :: error: (assignment)
    @Positive int e = 0;
    // :: error: (assignment)
    @Positive int f = -1;
    // :: error: (assignment)
    @Positive int g = -6;

    @NonNegative int h = 0;
    @GTENegativeOne int i = 0;
    @LowerBoundUnknown int j = 0;
    // :: error: (assignment)
    @NonNegative int k = -1;
    // :: error: (assignment)
    @NonNegative int l = -4;

    @GTENegativeOne int m = -1;
    @LowerBoundUnknown int n = -1;
    // :: error: (assignment)
    @GTENegativeOne int o = -9;
  }
}
// a comment
