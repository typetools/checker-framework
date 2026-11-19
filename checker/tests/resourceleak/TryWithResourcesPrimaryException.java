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

  static class OtherException extends Exception {}

  interface OtherCloseable extends AutoCloseable {
    @Override
    void close() throws OtherException;
  }

  public abstract @Owning OtherCloseable allocOther();

  // The try block always throws an IOException.  If close() throws an OtherException, then that
  // exception is suppressed, and the IOException propagates to the handler, which closes c.  An
  // OtherException never propagates from the try-with-resources statement.
  void primaryExceptionReachesHandler() throws IOException, OtherException {
    Closeable c = allocWithoutException();
    try {
      try (OtherCloseable r = allocOther()) {
        throw new IOException();
      }
    } catch (IOException e) {
      c.close();
    }
  }
}
