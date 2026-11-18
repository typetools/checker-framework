// Test case for a crash on `throw null` in a `try` block that has a `catch` clause.
// `throw null` throws a NullPointerException.

import org.checkerframework.checker.nullness.qual.Nullable;

public class ThrowNullInTry {

  void catchRuntimeException() {
    try {
      // :: error: [throwing.nullable]
      throw null;
    } catch (RuntimeException e) {
    }
  }

  String catchNullPointerException() {
    @Nullable String s = null;
    try {
      s = "hello";
      // :: error: [throwing.nullable]
      throw null;
    } catch (IllegalStateException e) {
      // This catch clause is unreachable, because a NullPointerException is not an
      // IllegalStateException.
      s = null;
    } catch (NullPointerException e) {
    }
    return s;
  }
}
