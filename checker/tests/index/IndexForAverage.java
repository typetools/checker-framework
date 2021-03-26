// test case for issue 86: https://github.com/kelloggm/checker-framework/issues/86

import org.checkerframework.checker.index.qual.*;

public class IndexForAverage {

  public static void bug(int[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
    @IndexFor("a") int k = (i + j) / 2;
  }

  public static void bug2(int[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
    @LTLengthOf("a") int k = ((i - 1) + j) / 2;
    // :: error: (assignment.type.incompatible)
    @LTLengthOf("a") int h = ((i + 1) + j) / 2;
  }
}
