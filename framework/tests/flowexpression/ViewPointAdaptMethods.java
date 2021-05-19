package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class ViewPointAdaptMethods {
  Object param1;

  void method1(Object param1, @FlowExp("#1") Object param2) {
    @FlowExp("param1") Object local = param2;
    @FlowExp("this.param1")
    // :: error: (assignment)
    Object local2 = param2;
    @FlowExp("#1") Object local3 = param2;
  }

  Object field;

  void callMethod1(@FlowExp("this.field") Object param, @FlowExp("#1") Object param2) {
    method1(field, param);
    // :: error: (argument)
    method1(field, param2);
  }

  @FlowExp("#2") Object method2(@FlowExp("#2") Object param1, Object param2, boolean flag) {
    if (flag) {
      return param1;
    } else if (param1 == param2) {
      @FlowExp("#2")
      // :: error: (assignment)
      Object o = new Object();
      return o;
    } else {
      @FlowExp("param2")
      // :: error: (assignment)
      Object o = new Object();
      return o;
    }
  }
}
