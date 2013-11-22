// @skip-test
// Test case for issue #277: http://code.google.com/p/checker-framework/issues/detail?id=277

import checkers.nullness.quals.*;

abstract class Ternary {
  void f(@Nullable Object o) {
    g(42, o != null ? o.hashCode() : 0);
  }

  abstract void g(Object x, Object xs);
}
