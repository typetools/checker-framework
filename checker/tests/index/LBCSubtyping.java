import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.LowerBoundUnknown;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class LBCSubtyping {

  void foo() {

    @GTENegativeOne int i = -1;

    @LowerBoundUnknown int j = i;

    int k = -4;

    // not this one though
    // :: error: (assignment)
    @GTENegativeOne int l = k;

    @NonNegative int n = 0;

    @Positive int a = 1;

    // check that everything is aboveboard
    j = a;
    j = n;
    l = n;
    n = a;

    // error cases

    // :: error: (assignment)
    @NonNegative int p = i;
    // :: error: (assignment)
    @Positive int b = i;

    // :: error: (assignment)
    @NonNegative int r = k;
    // :: error: (assignment)
    @Positive int c = k;

    // :: error: (assignment)
    @Positive int d = r;
  }
}
// a comment
