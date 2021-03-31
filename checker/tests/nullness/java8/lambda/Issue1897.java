// Test case for Issue 1897:
// https://github.com/typetools/checker-framework/issues/1897

import java.util.function.Function;

public class Issue1897 {
  Issue1897() {
    final int length = 1;
    takesLambda(s -> length);
  }

  static void takesLambda(Function<String, Integer> function) {}
}
