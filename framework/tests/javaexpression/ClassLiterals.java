package javaexpression;

import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class ClassLiterals {

  // In ordinary Java code, "String.class" is the same as "java.lang.String.class".
  // This declaration causes the two to be different.
  static class String {}

  void method(
      @FlowExp("String.class") Object p1,
      @FlowExp("String.class") Object p2,
      @FlowExp("java.lang.String.class") Object p3) {
    @FlowExp("String.class") Object l1 = p1;
    @FlowExp("String.class") Object l2 = p2;
    // :: error: (assignment)
    @FlowExp("String.class") Object l3 = p3;
    // :: error: (assignment)
    @FlowExp("java.lang.String.class") Object l4 = p1;
    // :: error: (assignment)
    @FlowExp("java.lang.String.class") Object l5 = p2;
    @FlowExp("java.lang.String.class") Object l6 = p3;
  }

  @FlowExp("void.class") String s0;

  @FlowExp("int.class") String s1;

  @FlowExp("int[].class") String s2;

  @FlowExp("String[].class") String s3;

  @FlowExp("java.lang.String[].class") String s4;
}
