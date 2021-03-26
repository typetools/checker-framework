// Test case for Issue 1984
// https://github.com/typetools/checker-framework/issues/1984

import org.checkerframework.common.value.qual.IntRange;

public class Issue1984 {
  public int m(int[] a, @IntRange(from = 0, to = 12) int i) {
    // :: error: (array.access.unsafe.high.range)
    return a[i];
  }
}
