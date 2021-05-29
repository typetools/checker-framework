// Test case for issue #277: https://github.com/typetools/checker-framework/issues/277

import org.checkerframework.checker.nullness.qual.*;

abstract class TernaryNullness {
  void f(@Nullable Object o) {
    g(42, o != null ? o.hashCode() : 0);
  }

  abstract void g(Object x, Object xs);
}
