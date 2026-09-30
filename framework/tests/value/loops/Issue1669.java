// Test case for https://github.com/typetools/checker-framework/issues/1669

import org.checkerframework.common.value.qual.IntRange;

public class Issue1669 {
  void test() {
    for (int i = 0; i < 12; i++) {
      @IntRange(from = 0, to = 12) int j = i;
      @IntRange(from = 0, to = 11) int k = i;
      // :: error: [assignment]
      @IntRange(from = 0, to = 10) int l = i;
    }
  }
}
