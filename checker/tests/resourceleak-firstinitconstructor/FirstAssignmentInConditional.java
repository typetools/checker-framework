// Demonstrates conservative behavior for assignments inside conditionals.
//
// Because the current analysis does not reason about control-flow branches,
// each assignment inside an if/else is treated as a potential re-assignment
// rather than the first write. As a result, every write below is reported,
// even though each branch actually assigns the field only once.
//
// A CFG-aware implementation would recognize that only one branch executes
// and would not warn on any of these assignments.

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class FirstAssignmentInConditional {
  private @Owning FileInputStream s;

  public FirstAssignmentInConditional(boolean b) {
    try {
      if (b) {
        // ::error: (required.method.not.called)
        s = new FileInputStream("test1.txt"); // false positive: first write in this branch
      } else {
        // ::error: (required.method.not.called)
        s = new FileInputStream("test2.txt"); // false positive: first write in this branch
      }
    } catch (Exception e) {
    }
  }

  public FirstAssignmentInConditional(boolean b1, boolean b2) {
    try {
      if (b1) {
        if (b2) {
          // ::error: (required.method.not.called)
          s = new FileInputStream("test1.txt"); // false positive
        } else {
          // ::error: (required.method.not.called)
          s = new FileInputStream("test2.txt"); // false positive
        }
      } else {
        if (b2) {
          // ::error: (required.method.not.called)
          s = new FileInputStream("test1.txt"); // false positive
        } else {
          // ::error: (required.method.not.called)
          s = new FileInputStream("test2.txt"); // false positive
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
      // ignore
    }
  }
}
