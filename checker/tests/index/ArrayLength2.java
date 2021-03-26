// Test case for issue #14:
// https://github.com/kelloggm/checker-framework/issues/14

import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.common.value.qual.MinLen;

public class ArrayLength2 {
  public static void main(String[] args) {
    int N = 8;
    int @MinLen(8) [] Grid = new int[N];
    @LTLengthOf("Grid") int i = 0;
  }
}
