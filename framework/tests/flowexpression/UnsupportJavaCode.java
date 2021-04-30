package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class UnsupportJavaCode {

  void method() {

    // :: error: (expression.unparsable)
    @FlowExp("new Object()") String s0;

    // :: error: (expression.unparsable)
    @FlowExp("List<String> list;") String s1;
  }
}
