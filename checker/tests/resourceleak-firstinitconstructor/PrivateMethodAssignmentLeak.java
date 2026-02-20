// Test: Field is assigned inside a private method called by constructor.
// Expected: Warning, since method-level assignment is not yet tracked.

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class PrivateMethodAssignmentLeak {
  private @Owning FileInputStream f;

  public PrivateMethodAssignmentLeak() {
    init();
  }

  // :: error: (missing.creates.mustcall.for)
  private void init() {
    try {
      // :: error: (required.method.not.called)
      f = new FileInputStream("file.txt");
    } catch (Exception e) {
    }
  }

  // :: error: (missing.creates.mustcall.for)
  public void open() {
    try {
      // :: error: (required.method.not.called)
      f = new FileInputStream("file.txt");
    } catch (Exception e) {
    }
  }

  @EnsuresCalledMethods(value = "this.f", methods = "close")
  public void close() {
    try {
      f.close();
    } catch (Exception e) {
    }
  }
}
