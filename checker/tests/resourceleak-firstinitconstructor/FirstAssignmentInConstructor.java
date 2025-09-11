// Test: Field has no initializer and is first assigned in constructor.
// Expected: No warning in constructor, warning in open().

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class FirstAssignmentInConstructor {
  private @Owning FileInputStream s;

  public FirstAssignmentInConstructor() {
    try {
      s = new FileInputStream("test.txt"); // no warning
    } catch (Exception e) {
    }
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
