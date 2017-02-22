import testlib.flowexpression.qual.FlowExp;

public class ValueLiterals {
    void test(@FlowExp("0") Object a, @FlowExp("0L") Object b) {}

    void test2(@FlowExp("1000") Object a, @FlowExp("100L") Object b) {}
    //:: error: (expression.unparsable.type.invalid)
    void test3(@FlowExp("01000") Object a) {}
    //:: error: (expression.unparsable.type.invalid)
    void test4(@FlowExp("0100L") Object b) {}
}
