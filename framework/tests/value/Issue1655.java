// Test case for Issue 1655
// https://github.com/typetools/checker-framework/issues/1655

import org.checkerframework.common.value.qual.IntRange;

public class Issue1655 {

  public void test(int a) {
    @IntRange(from = 0, to = 255) int b = a & 0xff;
    @IntRange(from = 0, to = 15) int c1 = b >> 4;
    @IntRange(from = 0, to = 15) int c2 = b >>> 4;
  }
}
