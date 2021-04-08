// Tests that polymorphic annotations are supported by the Accumulation Checker.

import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class SimplePolymorphic {
  @PolyTestAccumulation Object id(@PolyTestAccumulation Object obj) {
    return obj;
  }

  @TestAccumulation("foo") Object usePoly(@TestAccumulation("foo") Object obj) {
    return id(obj);
  }

  // Check that polymorphic supertype with accumulator type doesn't cause a crash.
  void noCrashOnPolySuper(@TestAccumulation("foo") Object obj) {
    // :: error: assignment.type.incompatible
    @PolyTestAccumulation Object obj2 = obj;
  }
}
