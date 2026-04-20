// Test for a field's reassignment within a constructor, where the assignments are within the
// brances
// of a conditional.

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

  public FirstAssignmentInConditional(int n) {
    try {
      if (n > 0) {
        s = new FileInputStream("test1.txt");
      }
    } catch (Exception e) {
    }
  }

  public FirstAssignmentInConditional(boolean b, int n) {
    try {
      if (b) {
        int x = n + 1;
      } else {
        int y = n + 2;
      }
      s = new FileInputStream("test1.txt");
    } catch (Exception e) {
    }
  }

  public FirstAssignmentInConditional(boolean b1, boolean b2, int n) {
    try {
      if (b1) {
        if (b2) {
          int x = n + 1;
        } else {
          int y = n + 2;
        }
      } else {
        int z = n + 3;
      }
      s = new FileInputStream("test1.txt");
    } catch (Exception e) {
    }
  }

  // This one should still be rejected: the write after the conditional is not definitely the first
  // write on all paths.
  public FirstAssignmentInConditional(boolean b, String ignored) {
    try {
      if (b) {
        s = new FileInputStream("test1.txt");
      }
      // ::error: [required.method.not.called]
      s = new FileInputStream("test2.txt");
    } catch (Exception e) {
    }
  }

  @EnsuresCalledMethods(value = "this.s", methods = "close")
  public void close() {
    try {
      s.close();
    } catch (Exception e) {
      // ignore
    }
  }
}
