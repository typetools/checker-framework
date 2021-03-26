package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class UnaryOperations {

  void method(int i, @FlowExp("+#1") String s) {
    @FlowExp("i") String q = s;
    @FlowExp("-9223372036854775808L") String s0;
  }
}
