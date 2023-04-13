// Test case for Issue #1209
// https://github.com/typetools/checker-framework/issues/1209

import org.checkerframework.checker.signedness.qual.PolySigned;

public class PolymorphicReturnType {

  public @PolySigned byte get() {
    // :: error: (return)
    return 0;
  }

  static @PolySigned int flip0(@PolySigned int value) {
    return Integer.MIN_VALUE;
  }

  static @PolySigned int flip(@PolySigned int value) {
    return value ^ Integer.MIN_VALUE;
  }
}
