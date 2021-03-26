// Test case for kelloggm#216
// https://github.com/kelloggm/checker-framework/issues/216

import org.checkerframework.common.value.qual.*;

public class NegativeIndex {
  @SuppressWarnings("lowerbound")
  void m(int[] a) {
    int x = a[-100];
  }
}
