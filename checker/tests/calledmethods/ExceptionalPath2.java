import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.*;

class ExceptionalPath2 {

  interface Resource {
    void a();

    void b() throws IOException;
  }

  Resource r;

  // Regression test for an obscure bug: in some cases, the called
  // methods transfer function would silently fail to update the
  // set of known called methods along exceptional paths.  That
  // would a spurious precondition error on this method.
  @EnsuresCalledMethods(
      value = "this.r",
      methods = {"b"})
  void test() {
    try {
      try {
        r.a();
      } finally {
        r.b();
      }
    } catch (IOException ignored) {
      // The only way to get here is if `r.b()` started running and
      // threw an IOException.  We no longer know whether `r.a()`
      // has been called, since `r.b()` might have overwritten `r`
      // before throwing.
      // ::error: (assignment)
      @CalledMethods({"a"}) Resource x = r;
    }
  }
}
