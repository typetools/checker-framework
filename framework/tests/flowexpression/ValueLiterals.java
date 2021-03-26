import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class ValueLiterals {
  void test(@FlowExp("0") Object a, @FlowExp("0L") Object b) {}

  void test2(@FlowExp("1000") Object a, @FlowExp("100L") Object b) {}

  void test3(@FlowExp("01000") Object a) {}

  void test4(@FlowExp("0100L") Object b) {}

  void test5(@FlowExp("0100l") Object b) {}
}
