// Test case for issue 93: https://github.com/kelloggm/checker-framework/issues/93

import org.checkerframework.checker.index.qual.*;

public class ArrayCreationParam {

  public static void m1() {
    int n = 5;
    int[] a = new int[n + 1];
    // Index Checker correctly issues no warning on the lines below
    @LTLengthOf("a") int j = n;
    @IndexFor("a") int k = n;
    for (int i = 1; i <= n; i++) {
      int x = a[i];
    }
  }
}
