// Test case for Issue 419:
// https://code.google.com/p/checker-framework/issues/detail?id=419

import org.checkerframework.checker.nullness.qual.*;

class Issue419 {
  @SuppressWarnings("nullness")
  <T> @NonNull T verifyNotNull(@Nullable T o) {
    return o;
  }

  interface Pair<A, B> {
    @Nullable A getFirst();
  }

  void m(Pair<String[], int[]> p) {
    for (String s : verifyNotNull(p.getFirst())) {
    }
  }
}
