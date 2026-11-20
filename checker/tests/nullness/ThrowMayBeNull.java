// Throwing a null value throws a NullPointerException, which goes to the matching `catch` clause,
// even if the static type of the thrown expression is not NullPointerException.

import org.checkerframework.checker.nullness.qual.Nullable;

public class ThrowMayBeNull {

  String throwNullVariable(@Nullable IllegalStateException e) {
    @Nullable String s = null;
    try {
      s = "hello";
      // :: error: [throwing.nullable]
      throw e;
    } catch (IllegalStateException x) {
    } catch (NullPointerException x) {
      // Reachable, because `e` might be null.
      s = null;
    }
    // :: error: [return]
    return s;
  }

  String throwNullVariableInOuterTry(@Nullable IllegalStateException e) {
    @Nullable String s = null;
    try {
      try {
        s = "hello";
        // :: error: [throwing.nullable]
        throw e;
      } catch (IllegalStateException x) {
      }
    } catch (NullPointerException x) {
      // Reachable, because `e` might be null.
      s = null;
    }
    // :: error: [return]
    return s;
  }

  static class MyException extends RuntimeException {
    String throwThis() {
      @Nullable String s = null;
      try {
        s = "hello";
        throw this;
      } catch (MyException x) {
      } catch (NullPointerException x) {
        // Unreachable, because `this` is never null.
        s = null;
      }
      return s;
    }
  }
}
