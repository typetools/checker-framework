import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

abstract class BasicTest {

  abstract Closeable alloc();

  abstract void method();

  public void runtimeExceptionManuallyThrown() throws IOException {
    // this code is obviously wrong, but RuntimeException is ignored by default
    Closeable r = alloc();
    if (true) {
      throw new RuntimeException();
    }
    r.close();
  }

  public void runtimeExceptionFromMethod() throws IOException {
    // method() may throw RuntimeException, but RuntimeException is ignored by default
    Closeable r = alloc();
    method();
    r.close();
  }

  public void ignoreIllegalStateException() throws IOException {
    // this code is obviously wrong, but it is allowed because our ignored exceptions list
    // includes IllegalStateException
    Closeable r = alloc();
    if (true) {
      throw new IllegalStateException();
    }
    r.close();
  }
}
