// Test: Field is initialized in one constructor and reassigned in another via this() chaining.
// Expected: Warning in constructor and open() due to reassignments.

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class ConstructorChainingLeak {
  private @Owning FileInputStream s;

  public ConstructorChainingLeak() throws Exception {
    this(42);
    // :: error: (required.method.not.called)
    s = new FileInputStream("test.txt");
  }

  private ConstructorChainingLeak(int x) throws Exception {
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
