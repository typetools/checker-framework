package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class Unparsable {
  // :: error: (expression.unparsable)
  @FlowExp("lsdjf") Object o3 = null;

  void method() {
    // :: error: (expression.unparsable)
    @FlowExp("new Object()") Object o1 = null;
    // :: error: (expression.unparsable)
    @FlowExp("int x = 0") Object o2 = null;
  }
}
