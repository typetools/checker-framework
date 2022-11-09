// A test that caused a ClassCastException in the UpperBound Checker. Based on a
// function in MathPlume, discovered while minimizing another crash in WPI (hence
// why the function from MathPlume was changed to just return 0 in the first place...).

import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.PolyUpperBound;

public final class MathPlumeClasscastCrash {

  @SuppressWarnings("index:return")
  public static @NonNegative @LessThan("#2") @PolyUpperBound long modPositive(
      long x, @PolyUpperBound long y) {
    return 0;
  }

}
