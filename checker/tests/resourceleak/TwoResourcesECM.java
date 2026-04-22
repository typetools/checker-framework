// A test case for https://github.com/typetools/checker-framework/issues/4838.
//
// This test that shows that no unsoundess occurs when a single close() method is responsible
// for closing two resources.

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("dispose")
class TwoResourcesECM {
  @Owning Socket s1, s2;

  // The contracts.exceptional.postcondition error is thrown because destructors
  // have to close their resources even on exception.  If s1.close() throws an
  // exception, then s2.close() will not be called.
  @EnsuresCalledMethods(
      value = {"this.s1", "this.s2"},
      methods = {"close"})
  // "contracts.postcondition" is a false positive warning, because no side effect should
  // unrefine the "@Closed" type of `s1`.
  // :: error: [contracts.postcondition]
  // :: error: [contracts.exceptional.postcondition]
  public void dispose() throws IOException {
    s1.close();
    s2.close();
  }

  static void test1(TwoResourcesECM obj) {
    try {
      obj.dispose();
    } catch (IOException ioe) {

    }
  }
}
