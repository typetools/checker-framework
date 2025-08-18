package javaexpression;

import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class Unparsable {
  // :: error: (expression.unparsable)
  @FlowExp("lsdjf") Object o3 = null;

  void method() {
    // :: error: (expression.unparsable)
    @FlowExp("new Object()") Object o1 = null;
    // :: error: (expression.unparsable)
    @FlowExp("int unparseable = 0") Object o2 = null;
  }
}
