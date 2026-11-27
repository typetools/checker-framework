import org.checkerframework.checker.nullness.qual.Nullable;

// A try-with-resources statement suppresses an exception that close() throws by calling
// addSuppressed() on the primary exception.  If close() throws the primary exception itself, then
// addSuppressed() throws an IllegalArgumentException, which replaces the primary exception.  That
// is possible only if close() might throw an exception that has a type of the primary exception.
public class TryWithResourcesSelfSuppression {

  static class MyException extends Exception {}

  interface Resource extends AutoCloseable {
    @Override
    void close();
  }

  interface ResourceThrowingMyException extends AutoCloseable {
    @Override
    void close() throws MyException;
  }

  void selfSuppression(Resource r, MyException ex) throws MyException {
    @Nullable Object o = "";
    try {
      try (r) {
        o = null;
        throw ex;
      }
    } catch (IllegalArgumentException e) {
      // The handler is reachable only through the addSuppressed() call.  If ex is null, the
      // primary exception is a NullPointerException, which close() might throw.
      // :: error: (dereference.of.nullable)
      o.toString();
    }
  }

  // In the next two methods, the primary exception is always a MyException:  the inner catch
  // clause catches every unchecked exception that the try block throws.  The inner catch clause
  // makes o non-null before the return statement, so o is non-null on the path on which the
  // normal-path close() throws an IllegalArgumentException.

  void selfSuppressionOfCheckedException(ResourceThrowingMyException r, MyException ex)
      throws MyException {
    @Nullable Object o = "";
    try {
      try (r) {
        try {
          o = null;
          throw ex;
        } catch (RuntimeException | Error e) {
          o = "";
          return;
        }
      }
    } catch (IllegalArgumentException e) {
      // close() might throw the primary exception itself, so the handler is reachable through the
      // addSuppressed() call.
      // :: error: (dereference.of.nullable)
      o.toString();
    }
  }

  void noSelfSuppression(Resource r, MyException ex) throws MyException {
    @Nullable Object o = "";
    try {
      try (r) {
        try {
          o = null;
          throw ex;
        } catch (RuntimeException | Error e) {
          o = "";
          return;
        }
      }
    } catch (IllegalArgumentException e) {
      // close() cannot throw a MyException, so it cannot throw the primary exception, and the
      // addSuppressed() call does not throw.
      o.toString();
    }
  }
}
