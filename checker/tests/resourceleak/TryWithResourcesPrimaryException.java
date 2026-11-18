// Tests that the Resource Leak Checker does not ignore the exception that propagates from a
// try-with-resources statement after the statement closes its resource and suppresses any
// exception that close() throws.  The exception is an IOException, which the Resource Leak Checker
// does not ignore.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;

abstract class TryWithResourcesPrimaryException {

  public abstract @Owning Closeable alloc() throws IOException;

  public abstract void mightThrow() throws IOException;

  void leakAfterTryWithResources() throws IOException {
    // :: error: [required.method.not.called]
    Closeable c = alloc();
    try (Closeable r = alloc()) {
      mightThrow();
    }
    c.close();
  }

  void closedAfterTryWithResourcesWithCatch() throws IOException {
    Closeable c = alloc();
    try (Closeable r = alloc()) {
      mightThrow();
    } catch (IOException e) {
      // The exception does not propagate.
    }
    c.close();
  }
}
