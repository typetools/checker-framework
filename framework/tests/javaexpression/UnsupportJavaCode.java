package javaexpression;

import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class UnsupportJavaCode {

  void method() {

    // :: error: (expression.unparsable)
    @FlowExp("new Object()") String s0;

    // :: error: (expression.unparsable)
    @FlowExp("List<String> list;") String s1;
  }
}
