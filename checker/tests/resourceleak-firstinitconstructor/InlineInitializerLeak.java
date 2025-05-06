// Test: Field has a non-null inline initializer and is reassigned in constructor and open().
// Expected: Warning in constructor and in open().

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class InlineInitializerLeak {
  private @Owning FileInputStream s = new FileInputStream("test.txt");

  public InlineInitializerLeak() throws Exception {
    // :: error: (required.method.not.called)
    s = new FileInputStream("test.txt");
  }

  // :: error: (missing.creates.mustcall.for)
  public void open() {
    try {
      // :: error: (required.method.not.called)
      s = new FileInputStream("test.txt");
    } catch (Exception e) {
    }
  }

  @EnsuresCalledMethods(value = "this.s", methods = "close")
  public void close() {
    try {
      s.close();
    } catch (Exception e) {
    }
  }
}
