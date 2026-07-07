// Test for field assignment in try-catch-finally paths.
// The field is assigned in all three paths, but only the finally path should warn,
// as it is always executed after field's initial assignment in either try/catch.

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class TryCatchFinallyPath {
  private @Owning FileInputStream s;
  static FileInputStream a, b, c;

  TryCatchFinallyPath(boolean fail) {
    try {
      if (!fail) {
        s = a; // OK on non-failing path
      } else {
        throw new RuntimeException();
      }
    } catch (Exception e) {
      s = b; // OK on failing path
    } finally {
      // ::error: [required.method.not.called]
      s = c; // correct warning: later write regardless of path
    }
  }

  @EnsuresCalledMethods(value = "this.s", methods = "close")
  public void close() {
    try {
      s.close();
    } catch (Exception e) {
      // ignore
    }
  }
}
