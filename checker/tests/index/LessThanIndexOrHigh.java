import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LessThan;

// The type of `end` is @IndexOrHigh("array"), which implies the @IntRangeFromNonNegative alias.
// `LessThanAnnotatedTypeFactory.getMinValueFromString` looks for @IntRange, so the Index Checker
// can prove that -1 is less than `end` only if the abstract value for `end` holds an @IntRange
// rather than the alias.  `ValueAnalysis` converts the alias whenever it creates an abstract value,
// not only when it merges two values at a control-flow join point; this method has no join point.
public class LessThanIndexOrHigh {

  private static @LessThan("#2") int f(short[] array, @IndexOrHigh("#1") int end) {
    return -1;
  }
}
