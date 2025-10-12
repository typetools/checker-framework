// Shows a limitation of the conservative (AST-only) first-write check.
//
// Without control-flow info, the analysis can’t tell that the `finally` block
// always runs after both `try` and `catch`. As a result, it flags both the write
// inside `try` and the one inside `finally`, even though only the latter should
// be reported. A CFG-aware analysis would warn only on the `finally` write.

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
        // ::error: (required.method.not.called)
        s = a; // falsely reported: a CFG-aware analysis would not warn here
      } else {
        throw new RuntimeException();
      }
    } catch (Exception e) {
      s = b; // OK on failing path
    } finally {
      // ::error: (required.method.not.called)
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
