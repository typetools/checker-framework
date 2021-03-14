// Test case for issue #14:
// https://github.com/kelloggm/checker-framework/issues/14

import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.common.value.qual.ArrayLen;

public class ArrayLength3 {
  String getFirst(String @ArrayLen(2) [] sa) {
    return sa[0];
  }

  void m() {
    Integer[] a = new Integer[10];
    @LTLengthOf("a") int i = 5;
  }
}
