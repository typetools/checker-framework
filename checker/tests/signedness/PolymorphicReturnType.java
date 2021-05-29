// Test case for Issue #1209
// https://github.com/typetools/checker-framework/issues/1209

import org.checkerframework.checker.signedness.qual.PolySigned;

public class PolymorphicReturnType {

  public @PolySigned byte get() {
    // :: error: (return)
    return 0;
  }
}
