import org.checkerframework.checker.nullness.qual.Nullable;

// A try-with-resources statement suppresses an exception that close() throws by calling
// addSuppressed() on the primary exception.  If close() throws the primary exception itself, then
// addSuppressed() throws an IllegalArgumentException, which replaces the primary exception.
public class TryWithResourcesSelfSuppression {

  static class MyException extends Exception {}

  interface Resource extends AutoCloseable {
    @Override
    void close();
  }

  void selfSuppression(Resource r, MyException ex) throws MyException {
    @Nullable Object o = "";
    try {
      try (r) {
        o = null;
        throw ex;
      }
    } catch (IllegalArgumentException e) {
      // The handler is reachable only through the addSuppressed() call.
      // :: error: (dereference.of.nullable)
      o.toString();
    }
  }
}
