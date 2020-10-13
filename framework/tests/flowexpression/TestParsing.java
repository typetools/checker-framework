import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class TestParsing {
    int[] a = {1, 2};

    void test(@FlowExp("a.length") Object a, @FlowExp("this.a.length") Object b) {}

    void test2(@FlowExp("a.clone()") Object a, @FlowExp("this.a.clone()") Object b) {}
    // :: error: (expression.unparsable.type.invalid)
    void test3(@FlowExp("a.leng") Object a) {}
}
