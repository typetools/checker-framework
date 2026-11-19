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
}
