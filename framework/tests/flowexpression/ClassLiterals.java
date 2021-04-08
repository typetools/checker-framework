package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class ClassLiterals {
  static class String {}

  void method(
      @FlowExp("String.class") Object p1,
      @FlowExp("String.class") Object p2,
      @FlowExp("java.lang.String.class") Object p3) {
    @FlowExp("String.class") Object l1 = p1;
    @FlowExp("String.class") Object l2 = p2;
    // :: error: (assignment.type.incompatible)
    @FlowExp("String.class") Object l3 = p3;
    // :: error: (assignment.type.incompatible)
    @FlowExp("java.lang.String.class") Object l4 = p1;
    // :: error: (assignment.type.incompatible)
    @FlowExp("java.lang.String.class") Object l5 = p2;
    @FlowExp("java.lang.String.class") Object l6 = p3;
  }

  @FlowExp("void.class") String s0;

  @FlowExp("int.class") String s1;

  @FlowExp("int[].class") String s2;

  @FlowExp("String[].class") String s3;

  @FlowExp("java.lang.String[].class") String s4;
}
