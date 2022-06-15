// Another test that must-call close errors are not issued when overwriting a field
// if the field is definitely null.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

@MustCall("close") class NonFinalFieldOnlyOverwrittenIfNull2 {
  @Owning @MonotonicNonNull InputStream is;

  @CreatesMustCallFor
  void set(String fn) throws FileNotFoundException {
    if (is == null) {
      is = new FileInputStream(fn);
    }
  }

  @CreatesMustCallFor
  void set_after_close(String fn, boolean b) throws IOException {
    if (b) {
      is.close();
      is = new FileInputStream(fn);
    }
  }

  @CreatesMustCallFor
  void set_error(String fn, boolean b) throws FileNotFoundException {
    if (b) {
      // :: error: required.method.not.called
      is = new FileInputStream(fn);
    }
  }

  /* This version of close() doesn't verify, because in the `catch` block
     `is` isn't @CalledMethods("close"). TODO: investigate that in the CM checker
  @EnsuresCalledMethods(value="this.is", methods="close")
  void close_real() {
      if (is != null) {
          try {
              is.close();
          } catch (Exception ie) {

          }
      }
  } */

  @EnsuresCalledMethods(value = "this.is", methods = "close")
  @CreatesMustCallFor
  void close() throws Exception {
    if (is != null) {
      is.close();
      is = null;
    }
  }

  public static void test_leak() throws Exception {
    // :: error: required.method.not.called
    NonFinalFieldOnlyOverwrittenIfNull2 n = new NonFinalFieldOnlyOverwrittenIfNull2();
    n.set("foo.txt");
    n.close();
    n.set("bar.txt");
  }
}
