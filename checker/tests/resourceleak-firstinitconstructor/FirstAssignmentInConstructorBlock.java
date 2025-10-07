// Test: Field has no initializer and is first assigned in constructor, but in a constructor block.
// Expected: No warning in the first assignment in the constructor block, warning in any later
// assignments in the constructor and in open().

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class FirstAssignmentInConstructorBlock {
  private @Owning FileInputStream s;

  static FileInputStream s2;
  static FileInputStream s3;

  public FirstAssignmentInConstructorBlock() {
    s = s2;
  }

  public FirstAssignmentInConstructorBlock(boolean b) {
    {
      s = s2;
    }
  }

  public FirstAssignmentInConstructorBlock(int i) {
    {
      s = s2;
    }
    // :: error: (required.method.not.called)
    s = s2;
  }

  public FirstAssignmentInConstructorBlock(float f) {
    s = s2;
    {
      // :: error: (required.method.not.called)
      s = s2;
    }
  }

  public FirstAssignmentInConstructorBlock(byte b) {
    {
      s = s3;
    }
    // :: error: (required.method.not.called)
    s = s2;
  }

  public FirstAssignmentInConstructorBlock(char c) {
    s = s2;
    {
      // :: error: (required.method.not.called)
      s = s3;
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
