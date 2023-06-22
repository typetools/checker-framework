// Test case for issue 2264
// https://github.com/typetools/checker-framework/issues/2264

import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;

public class Issue2264 extends SuperClass {
  // :: warning: (inconsistent.constructor.type)
  @H1S1 Issue2264() {
    // :: error: (super.invocation)
    super(9);
  }
}

class ImplicitSuperCall {
  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  @H1S1 ImplicitSuperCall() {}
}

class SuperClass {
  @H1Top SuperClass(int x) {}
}

@H1S1 class TestClass {
  // :: error: (annotations.on.use)
  @H1Top TestClass() {}
}
