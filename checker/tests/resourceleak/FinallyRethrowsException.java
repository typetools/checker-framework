// Tests that an exception that a finally block rethrows is not ignored.  The exceptions are an
// IOException and a MyChecked, which the Resource Leak Checker does not ignore.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;

abstract class FinallyRethrowsException {

  /** A resource whose close() method throws no checked exception. */
  interface QuietResource extends AutoCloseable {
    @Override
    void close();
  }

  public abstract @Owning Closeable alloc() throws IOException;

  public abstract @Owning QuietResource allocQuiet();

  /**
   * A resource whose close() method throws no checked exception, and which the Resource Leak
   * Checker requires to be closed.
   */
  interface QuietCloseable extends Closeable {
    @Override
    void close();
  }

  public abstract @Owning QuietCloseable allocQuietCloseable();

  /** A checked exception that the Resource Leak Checker does not ignore. */
  static class MyChecked extends Exception {}

  public abstract void mightThrow() throws IOException;

  public abstract void mightThrowMyChecked() throws MyChecked;

  public abstract void log();

  void leakAfterTryFinally() throws IOException {
    // :: error: [required.method.not.called]
    Closeable c = alloc();
    try {
      mightThrow();
    } finally {
      log();
    }
    c.close();
  }

  void leakAfterNestedTryFinally() throws IOException {
    // :: error: [required.method.not.called]
    Closeable c = alloc();
    try {
      try {
        mightThrow();
      } finally {
        log();
      }
    } finally {
      log();
    }
    c.close();
  }

  void leakAfterTryWithResources() throws IOException {
    // :: error: [required.method.not.called]
    Closeable c = alloc();
    try (QuietResource r = allocQuiet()) {
      mightThrow();
    }
    c.close();
  }

  void closedInFinally() throws IOException {
    Closeable c = alloc();
    try {
      mightThrow();
    } finally {
      c.close();
    }
  }

  void caughtBeforeFinally() throws IOException {
    Closeable c = alloc();
    try {
      mightThrow();
    } catch (IOException e) {
      // The exception does not reach the finally block.
    } finally {
      log();
    }
    c.close();
  }

  void rethrowCaughtByOuterCatch() throws IOException {
    Closeable c = alloc();
    try {
      try {
        mightThrow();
      } finally {
        log();
      }
    } catch (IOException e) {
      // The IOException that the finally block rethrows reaches this catch block.
      c.close();
      throw e;
    }
    c.close();
  }

  void closedOnOneExceptionalPath() throws MyChecked {
    // An IOException reaches the finally block while r is open, and the outer catch block closes
    // r.  A MyChecked exception reaches the finally block after r is closed, and it propagates out
    // of the method.  So r is closed on every path.  However, one copy of the finally block serves
    // both exceptions, so the end of that copy might throw MyChecked while r is open.
    // TODO: This error is a false positive.
    // :: error: [required.method.not.called]
    QuietCloseable r = allocQuietCloseable();
    try {
      try {
        mightThrow();
        r.close();
        mightThrowMyChecked();
      } finally {
        log();
      }
    } catch (IOException e) {
      r.close();
    }
  }
}
