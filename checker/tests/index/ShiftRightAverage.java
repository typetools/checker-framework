// Test case for kelloggm 217
// https://github.com/kelloggm/checker-framework/issues/217

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class ShiftRightAverage {
  public static void m(Object[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
    @IndexFor("#1") int k = (i + j) >> 1;
  }

  public static void m2(int[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
    // :: error: (assignment.type.incompatible)
    @LTLengthOf("a") int h = ((i + 1) + j) >> 1;
  }
}
