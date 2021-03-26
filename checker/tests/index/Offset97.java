// Test case for issue 97: https://github.com/kelloggm/checker-framework/issues/97

import org.checkerframework.checker.index.qual.*;

public class Offset97 {
  public static void m2() {
    int[] a = {1, 2, 3, 4, 5};
    @IndexFor("a") int i = 4;
    @IndexFor("a") int j = 4;
    if (j < a.length - i) {
      @IndexFor("a") int k = i + j;
    }
  }
}
