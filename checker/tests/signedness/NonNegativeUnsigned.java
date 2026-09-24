import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.common.value.qual.IntRange;

// A value that is @NonNegative or @Positive has its most significant bit clear, so it is
// @SignedPositive whether its type is @Signed or @Unsigned.
public class NonNegativeUnsigned {

  void nonNegativeParam(@NonNegative @Unsigned int u) {
    @Signed int s = u;
  }

  void positiveParam(@Positive @Unsigned int u) {
    @Signed int s = u;
  }

  void nonNegativeParamAfterJoin(@NonNegative @Unsigned int u, boolean b) {
    if (b) {
      u = u + 0;
    }
    @Signed int s = u;
  }

  void nonNegativeSigned(@NonNegative int i) {
    @Signed int s = i;
  }

  void unsignedParam(@Unsigned int u) {
    // :: error: [assignment]
    @Signed int s = u;
  }

  // A @PolySigned value is not refined to @SignedPositive, which would lose the polymorphism.
  @PolySigned int polyNonNegative(@PolySigned @NonNegative int x) {
    return x;
  }

  @PolySigned int polyIntRange(@PolySigned @IntRange(from = 0, to = 5) int x) {
    return x;
  }
}
