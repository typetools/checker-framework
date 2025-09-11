// Test: Field is explicitly initialized to null and assigned in constructor.
// Expected: No warning in constructor, warning in open().

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class ExplicitNullInitializer {
  private @Owning FileInputStream s = null;

  public ExplicitNullInitializer() {
    try {
      s = new FileInputStream("test.txt");
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
