import org.checkerframework.checker.nullness.qual.*;

public class Exceptions {
  void exceptionParam(@Nullable Exception m) {
    // :: error: [dereference.of.nullable]
    m.getClass(); // should emit error
  }

  void nonnullExceptionParam(@NonNull Exception m) {
    m.getClass();
  }

  void exception(@Nullable Exception m) {
    try {
      throwException();
    } catch (Exception e) {
      e.getClass();
      // :: error: [dereference.of.nullable]
      m.getClass(); // should emit error
    }
  }

  void throwException() {
    int a = 0;
    if (a == 0) {
      // :: error: [throwing.nullable]
      throw null;
    } else if (a == 1) {
      RuntimeException e = null;
      // :: error: [throwing.nullable]
      throw e;
    } else {
      RuntimeException e = new RuntimeException();
      throw e;
    }
  }

  void reassignException() {
    try {
      throwException();
    } catch (RuntimeException e) {
      // :: error: [assignment]
      e = null;
      throw e;
    }
  }

  // `throw null` throws a NullPointerException.
  void throwNullInTryCatch() {
    try {
      // :: error: [throwing.nullable]
      throw null;
    } catch (NullPointerException e) {
    }
  }

  void throwNullInTryFinallyInTryCatch() {
    try {
      try {
        // :: error: [throwing.nullable]
        throw null;
      } finally {
      }
    } catch (IllegalStateException e) {
    }
  }

  // The NullPointerException from `throw null` reaches the matching catch clause.  If it did not,
  // the catch clause would be unreachable and its body would not be checked.
  void throwNullReachesMatchingCatch() {
    @Nullable Object o = "";
    try {
      o = null;
      // :: error: [throwing.nullable]
      throw null;
    } catch (NullPointerException e) {
      // :: error: [dereference.of.nullable]
      o.toString();
    }
  }

  // Likewise, through a finally block.
  void throwNullInTryFinallyReachesMatchingCatch() {
    @Nullable Object o = "";
    try {
      try {
        // :: error: [throwing.nullable]
        throw null;
      } finally {
        o = null;
      }
    } catch (NullPointerException e) {
      // :: error: [dereference.of.nullable]
      o.toString();
    }
  }

  // In contrast, a local that remains non-null is non-null in the catch clause.
  void throwNullInTryFinallyReachesMatchingCatchNonNull() {
    @Nullable Object o = "";
    try {
      try {
        // :: error: [throwing.nullable]
        throw null;
      } finally {
        o = "";
      }
    } catch (NullPointerException e) {
      o.toString();
    }
  }
}
