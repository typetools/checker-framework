// Test: Method call appears before assignment in constructor.
// Expected: Warning in constructor (because method might modify the field), and also in open().

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class MethodCallBeforeAssignment {
  private @Owning FileInputStream s;

  public MethodCallBeforeAssignment() throws Exception {
    doSomething(); // Method call before assignment — suppressor should bail
    // :: error: (required.method.not.called)
    s = new FileInputStream("test.txt");
  }

  public MethodCallBeforeAssignment(boolean its_a_constructor_call) throws Exception {
    new C();
    // :: error: (required.method.not.called)
    s = new FileInputStream("test.txt");
  }

  private void doSomething() {
    System.out.println("placeholder");
  }

  // :: error: (missing.creates.mustcall.for)
  public void open() throws Exception {
    // :: error: (required.method.not.called)
    s = new FileInputStream("test.txt");
  }

  @EnsuresCalledMethods(value = "this.s", methods = "close")
  public void close() {
    try {
      s.close();
    } catch (Exception e) {
    }
  }

  class C {}
}
