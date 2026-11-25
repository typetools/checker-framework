package javaexpression;

import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class TypeVariableBoxedBound<T extends Integer, B extends Boolean> {

  void sum(T t, int j, @FlowExp("#1+#2") String s) {
    @FlowExp("t+j") String q = s;
  }

  void negationSum(T t, int j, @FlowExp("-#1+#2") String s) {
    @FlowExp("-t+j") String q = s;
  }

  void and(B b, boolean c, @FlowExp("#1&&#2") String s) {
    @FlowExp("b&&c") String q = s;
  }
}
