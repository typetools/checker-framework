package javaexpression;

import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class BoxedBinaryOperations {

  void sum(Integer i, long j, @FlowExp("#1+#2") String s) {
    @FlowExp("i+j") String q = s;
  }

  void lessThan(Integer i, Integer j, @FlowExp("#1<#2") String s) {
    @FlowExp("i<j") String q = s;
  }

  void and(Boolean b, boolean c, @FlowExp("#1&&#2") String s) {
    @FlowExp("b&&c") String q = s;
  }
}
