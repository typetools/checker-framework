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

  // Reference equality does not unbox its operands, so their reference types must be compatible.
  void referenceEquality(Integer i, Long l) {
    // :: error: [expression.unparsable]
    @FlowExp("i == l") Object o = null;
  }

  void shiftChar(Character c, long d, @FlowExp("#1<<#2") String s) {
    @FlowExp("c<<d") String q = s;
  }
}
