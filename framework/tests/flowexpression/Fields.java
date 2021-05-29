package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class Fields {
  static class String {
    public static final java.lang.String HELLO = "hello";
  }

  void method(
      // :: error: (expression.unparsable)
      @FlowExp("java.lang.String.HELLO") Object p1, @FlowExp("Fields.String.HELLO") Object p2) {
    // :: error: (assignment)
    @FlowExp("String.HELLO") Object l1 = p1;
    @FlowExp("String.HELLO") Object l2 = p2;
    @FlowExp("flowexpression.Fields.String.HELLO") Object l3 = p2;
  }
}
