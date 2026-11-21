// Tests that the Resource Leak Checker does not ignore the exception that propagates from a
// try-with-resources statement after the statement closes its resource and suppresses any
// exception that close() throws.  The exception is an IOException, which the Resource Leak Checker
// does not ignore.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.dataflow.qual.TerminatesExecution;

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

  // A resource whose close() terminates execution by throwing an Error, which the Resource Leak
  // Checker ignores.  The try-with-resources statement suppresses that Error and rethrows the
  // primary exception.
  static class TerminatingClose implements Closeable {
    @Override
    @TerminatesExecution
    public void close() {
      throw new Error();
    }
  }

  void leakWhenTryBlockThrowsAndCloseTerminates() throws IOException {
    // :: error: [required.method.not.called]
    Closeable c = alloc();
    try (TerminatingClose r = new TerminatingClose()) {
      // If mightThrow() throws, c is not closed.
      mightThrow();
      c.close();
    }
  }
}
