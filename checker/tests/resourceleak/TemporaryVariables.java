// For some expressions like `alloc()`, the Resource Leak Checker creates anonymous "temporary
// variables" to track must-call obligations.  These tests exercise some interesting interactions
// involving temporary variables.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

abstract class TemporaryVariables {
  public abstract Closeable alloc();

  public void test1() throws IOException {
    alloc().close();
  }

  // Identical to test1() but with explicit exception handling.
  public void test2() throws IOException {
    try {
      alloc().close();
    } catch (IOException e) {
      throw e;
    }
  }

  @EnsuresCalledMethods(value = "#1", methods = "close")
  @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
  public void close(Closeable x) throws IOException {
    x.close();
  }
}
