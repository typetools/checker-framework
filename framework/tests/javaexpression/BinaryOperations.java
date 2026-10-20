package javaexpression;

import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class BinaryOperations {

  void method(int i, int j, @FlowExp("#1+#2") String s) {
    @FlowExp("i+j") String q = s;
  }
}
