// Tests that the Resource Leak Checker does not ignore the exception that propagates from a
// try-with-resources statement after the statement closes its resource and suppresses any
// exception that close() throws.  The exception is an IOException, which the Resource Leak Checker
// does not ignore.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException;
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

  // A resource whose close() promises to call finish() if close() throws an Exception, but which
  // terminates execution by throwing an Error without calling finish().
  @InheritableMustCall({"close", "finish"})
  @SuppressWarnings(
      "declaration.inconsistent.with.implements.clause") // stronger @InheritableMustCall
  abstract static class FinishingTerminatingClose implements Closeable {
    abstract void finish();

    @Override
    @TerminatesExecution
    @EnsuresCalledMethodsOnException(value = "this", methods = "finish")
    public abstract void close();
  }

  abstract @Owning FinishingTerminatingClose allocFinishing();

  void finishNotCalledWhenTryBlockThrowsAndCloseTerminates() throws IOException {
    // If mightThrow() throws an IOException, then close() throws an Error without calling
    // finish(), and the try-with-resources statement rethrows the IOException.
    // :: error: [required.method.not.called]
    try (FinishingTerminatingClose r = allocFinishing()) {
      mightThrow();
      r.finish();
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
  // leak on the path through the CloseException handler.  The IllegalArgumentException handler
  // closes c too:  the statement suppresses the CloseException by calling addSuppressed(), which
  // throws an IllegalArgumentException if close() threw the primary exception itself.
  void primaryExceptionNotReplaced() throws IOException {
    Closeable c = allocWithoutException();
    try {
      try (ResourceWithCloseException r = allocWithCloseException()) {
        alwaysThrows();
        throw new Error();
      }
    } catch (IOException | IllegalArgumentException e) {
      c.close();
    } catch (CloseException e) {
      // unreachable
    }
  }
}
