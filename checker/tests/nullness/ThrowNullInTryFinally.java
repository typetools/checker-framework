// `throw null` in a `try` block that has a `finally` block throws a NullPointerException, which the
// `finally` block rethrows to the matching `catch` clause of an enclosing `try` statement.

import org.checkerframework.checker.nullness.qual.Nullable;

public class ThrowNullInTryFinally {

  String catchInOuterTry() {
    @Nullable String s = null;
    try {
      try {
        s = "hello";
        // :: error: [throwing.nullable]
        throw null;
      } finally {
        s = "goodbye";
      }
    } catch (IllegalStateException e) {
      // This catch clause is unreachable, because a NullPointerException is not an
      // IllegalStateException.
      s = null;
    } catch (NullPointerException e) {
    }
    return s;
  }

  String catchInOuterTryNullPointerExceptionReachable() {
    @Nullable String s = null;
    try {
      try {
        s = "hello";
        // :: error: [throwing.nullable]
        throw null;
      } finally {
        s = "goodbye";
      }
    } catch (IllegalStateException e) {
    } catch (NullPointerException e) {
      s = null;
    }
    // :: error: [return]
    return s;
  }

  // The exception propagates from the inner catch clause through the finally block.  The finally
  // block cannot throw a NullPointerException itself.
  void caughtAfterFinally(@Nullable Object o) {
    @Nullable String s = null;
    try {
      try {
        throw new RuntimeException();
      } catch (RuntimeException e) {
        o = "hello";
        // :: error: [throwing.nullable]
        throw null;
      } finally {
        s = "goodbye";
      }
    } catch (NullPointerException e) {
      o.toString();
    }
  }
}
