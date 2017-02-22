import testlib.flowexpression.qual.FlowExp;

public class ValueLiterals {
    void test(@FlowExp("0") Object a, @FlowExp("0L") Object b) {}
}
