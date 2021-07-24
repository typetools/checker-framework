// A test that multiple @RequiresNonNull annotations can be written on the same
// method and work correctly.

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.RequiresQualifier;

class RepeatedRequiresNonNull {
  @Nullable Object f1;
  @Nullable Object f2;

  @RequiresNonNull("this.f1")
  @RequiresNonNull("this.f2")
  void test() {
    f1.toString();
    f2.toString();
  }

  void use1() {
    // :: error: (contracts.precondition)
    test();
  }

  void use2() {
    if (this.f1 != null) {
      // :: error: (contracts.precondition)
      test();
    }
  }

  void use3() {
    if (this.f2 != null) {
      // :: error: (contracts.precondition)
      test();
    }
  }

  void use4() {
    if (this.f1 != null && this.f2 != null) {
      test();
    }
  }

  // This part of the test is to ensure that @RequiresNonNull and @RequiresQualifier behave
  // the same way. It is identical, but uses @RequiresQualifier on the test2() method instead
  // of the @RequiresNonNull on the test() method.

  @RequiresQualifier(expression = "this.f1", qualifier = NonNull.class)
  @RequiresQualifier(expression = "this.f2", qualifier = NonNull.class)
  void test2() {
    f1.toString();
    f2.toString();
  }

  void use21() {
    // :: error: (contracts.precondition)
    test2();
  }

  void use22() {
    if (this.f1 != null) {
      // :: error: (contracts.precondition)
      test2();
    }
  }

  void use23() {
    if (this.f2 != null) {
      // :: error: (contracts.precondition)
      test2();
    }
  }

  void use24() {
    if (this.f1 != null && this.f2 != null) {
      test2();
    }
  }
}
