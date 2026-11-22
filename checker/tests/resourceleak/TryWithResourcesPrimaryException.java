// Tests that the Resource Leak Checker does not ignore the exception that propagates from a
// try-with-resources statement after the statement closes its resource and suppresses any
// exception that close() throws.  The exception is an IOException, which the Resource Leak Checker
// does not ignore.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;

abstract class TryWithResourcesPrimaryException {

  public abstract @Owning Closeable alloc() throws IOException;

  public abstract @Owning Closeable allocWithoutException();

  public abstract void mightThrow() throws IOException;

  void leakWhenTryBlockThrows() throws IOException {
    // :: error: [required.method.not.called]
    Closeable c = alloc();
    try (Closeable r = allocWithoutException()) {
      // If mightThrow() throws, c is not closed.
      mightThrow();
      c.close();
    }
  }

  void closedInTryBlock() throws IOException {
    Closeable c = alloc();
    try (Closeable r = allocWithoutException()) {
      c.close();
      mightThrow();
    }
  }

  static class CloseException extends Exception {}

  interface ResourceWithCloseException extends AutoCloseable {
    @Override
    void close() throws CloseException;
  }

  public abstract @Owning ResourceWithCloseException allocWithCloseException();

  public abstract void alwaysThrows() throws IOException;

  // The try block always throws an IOException.  If close() throws a CloseException, the statement
  // suppresses it and rethrows the IOException, so the CloseException handler is unreachable.  The
  // IOException handler closes c.  If the CloseException replaced the IOException, then c would
  // leak on the path through the CloseException handler.
  void primaryExceptionNotReplaced() throws IOException {
    Closeable c = allocWithoutException();
    try {
      try (ResourceWithCloseException r = allocWithCloseException()) {
        alwaysThrows();
        throw new Error();
      }
    } catch (IOException e) {
      c.close();
    } catch (CloseException e) {
      // unreachable
    }
  }
}
