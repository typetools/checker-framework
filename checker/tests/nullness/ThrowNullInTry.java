// `throw null` in a `try` block throws a NullPointerException, which goes to the matching `catch`
// clause.

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

  String catchNullPointerExceptionInOuterTry() {
    @Nullable String s = null;
    try {
      try {
        s = "hello";
        // :: error: [throwing.nullable]
        throw null;
      } catch (IllegalStateException e) {
        // This catch clause is unreachable, because a NullPointerException is not an
        // IllegalStateException.
        s = null;
      }
    } catch (NullPointerException e) {
    }
    return s;
  }
}
