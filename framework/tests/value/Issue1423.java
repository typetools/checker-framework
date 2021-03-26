// Test case for Issue 1423:
// https://github.com/typetools/checker-framework/issues/1423

import org.checkerframework.common.value.qual.IntRange;

public class Issue1423 {
  void loop(int i) {
    int a = 0;
    while (i >= 2) {
      @IntRange(from = 2) int i2 = i;
      ++a;
    }
  }
}
