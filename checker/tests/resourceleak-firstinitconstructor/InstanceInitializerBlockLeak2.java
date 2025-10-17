// Test: Field is assigned in an instance initializer block and reassigned later.
// Expected: Warning in initializer block, constructor and open().

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class InstanceInitializerBlockLeak2 {
  private @Owning FileInputStream f;

  {
    assignF();
  }

  // :: error: (missing.creates.mustcall.for)
  void assignF() {
    try {
      // :: error: (required.method.not.called)
      f = new FileInputStream("file.txt");
    } catch (Exception e) {
    }
  }

  public InstanceInitializerBlockLeak2() {
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
