// Test case for eisop issue 24:
// https://github.com/eisop/checker-framework/issues/24

// This test case is extracted from ConcurrentHashMap.java,
// namely from the method `compute`.

import org.checkerframework.checker.nullness.qual.PolyNull;

public class InfiniteLoopIsSameType {
  private interface Intf1<R> {
    R apply();
  }

  public void compute(
      // checker works fine if not annotated
      Intf1<? extends @PolyNull Object> remappingFunction) {
    // must assign null
    Object nullval = null;
    for (; ; ) {
      // must assign to the null object
      nullval = remappingFunction.apply();
      // break must be in an if statement
      if (true) {
        break;
      }
    }
  }
}
