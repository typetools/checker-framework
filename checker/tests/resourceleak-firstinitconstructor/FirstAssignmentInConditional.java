import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class FirstAssignmentInConditional {
  private @Owning FileInputStream s;

  public FirstAssignmentInConditional(boolean b) {
    try {
      if (b) {
        s = new FileInputStream("test1.txt");
      } else {
        s = new FileInputStream("test2.txt");
      }
    } catch (Exception e) {
    }
  }

  public FirstAssignmentInConditional(boolean b1, boolean b2) {
    try {
      if (b1) {
        if (b2) {
          s = new FileInputStream("test1.txt");
        } else {
          s = new FileInputStream("test2.txt");
        }
      } else {
        if (b2) {
          s = new FileInputStream("test1.txt");
        } else {
          s = new FileInputStream("test2.txt");
        }
      }
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
