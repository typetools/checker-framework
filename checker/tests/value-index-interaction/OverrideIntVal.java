import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.IntVal;

public class OverrideIntVal {

  @NonNegative int foo(@IntVal(0) int zero) {
    return zero;
  }

  @NonNegative int bar(@BottomVal int bottom) {
    return bottom;
  }

  @NonNegative int m() {
    return 0;
  }

  @IntVal({0, 1, 2, 3}) int m2() {
    return 0;
  }

  @GTENegativeOne int n() {
    return -1;
  }

  @Positive int p() {
    return 1;
  }
}

class OverrideIntValSub extends OverrideIntVal {
  @Override
  @IntVal(0) int m() {
    return 0;
  }

  @Override
  @IntVal(0) int m2() {
    return 0;
  }

  @Override
  @IntVal(0) int n() {
    return 0;
  }

  @Override
  @IntVal(2) int p() {
    return 2;
  }
}

class OverrideIntValBottom extends OverrideIntVal {
  @Override
  @BottomVal int m() {
    throw new Error("never returns normally");
  }

  @Override
  @BottomVal int m2() {
    throw new Error("never returns normally");
  }
}
