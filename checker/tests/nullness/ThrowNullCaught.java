import org.checkerframework.checker.nullness.qual.Nullable;

// `throw null` throws a NullPointerException, which a catch clause can catch.
public class ThrowNullCaught {

  void caughtDirectly(@Nullable Object o) {
    try {
      o = "hello";
      // :: error: [throwing.nullable]
      throw null;
    } catch (NullPointerException e) {
      o.toString();
    }
  }

  // The exception propagates from the inner catch clause through the finally block.
  void caughtAfterFinally(@Nullable Object o) {
    try {
      try {
        throw new RuntimeException();
      } catch (RuntimeException e) {
        o = "hello";
        // :: error: [throwing.nullable]
        throw null;
      } finally {
        System.out.println();
      }
    } catch (NullPointerException e) {
      o.toString();
    }
  }

  // Throwing a variable whose value is null throws a NullPointerException, even if the variable's
  // declared type is another exception type.
  void variableCaughtAfterFinally(java.io.IOException ex) throws java.io.IOException {
    @Nullable Object o = "hello";
    try {
      try {
        o = null;
        throw ex;
      } finally {
        System.out.println();
      }
    } catch (NullPointerException e) {
      // :: error: (dereference.of.nullable)
      o.toString();
    }
  }
}
