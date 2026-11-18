// Tests that an exception that a finally block rethrows is not ignored.  The exception is an
// IOException, which the Resource Leak Checker does not ignore.

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

  public abstract void mightThrow() throws IOException;

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
}
