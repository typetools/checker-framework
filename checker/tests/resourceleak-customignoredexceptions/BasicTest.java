import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

abstract class BasicTest {

  abstract Closeable alloc();

  abstract void method();

  public void runtimeExceptionManuallyThrown() throws IOException {
    // this code is obviously wrong
    // ::error: (required.method.not.called)
    Closeable r = alloc();
    if (true) {
      throw new RuntimeException();
    }
    r.close();
  }

  public void runtimeExceptionFromMethod() throws IOException {
    // method() may throw RuntimeException, so this code is not OK
    // ::error: (required.method.not.called)
    Closeable r = alloc();
    method();
    r.close();
  }

  // Note that even just constructing an instance of NullPointerException can throw all kinds
  // of exceptions: ClassCircularityError, OutOfMemoryError, etc.  Even RuntimeException is
  // possible in theory.  So, to really test what we're trying to test, we have to isolate
  // the construction of the exception out here.
  static final NullPointerException NPE = new NullPointerException();

  public void ignoreNPE() throws IOException {
    // this code is obviously wrong, but it is allowed because our ignored exceptions list
    // includes NullPointerException
    Closeable r = alloc();
    if (true) {
      throw NPE;
    }
    r.close();
  }
}
