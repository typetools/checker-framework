// Test case for issue 98: https://github.com/kelloggm/checker-framework/issues/98

import org.checkerframework.checker.index.qual.*;

public class SubtractingNonNegatives {
  public static void m4(int[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
    int k = i;
    if (k >= j) {
      @IndexFor("a") int y = k;
    }
    for (k = i; k >= j; k -= j) {
      @IndexFor("a") int x = k;
    }
  }

  @SuppressWarnings("lowerbound")
  void test(int[] a, @Positive int y) {
    @LTLengthOf("a") int x = a.length - 1;
    @LTLengthOf(
        value = {"a", "a"},
        offset = {"0", "y"})
    int z = x - y;
    a[z + y] = 0;
  }

  @SuppressWarnings("lowerbound")
  void test2(int[] a, @Positive int y) {
    @LTLengthOf("a") int x = a.length - 1;
    int z = x - y;
    a[z + y] = 0;
  }
}
