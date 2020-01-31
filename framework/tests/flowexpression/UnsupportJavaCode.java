package flowexpression;

import testlib.flowexpression.qual.FlowExp;

public class UnsupportJavaCode {

    void method() {

        // :: error: (expression.unparsable.type.invalid)
        @FlowExp("new Object()") String s0;

        // :: error: (expression.unparsable.type.invalid)
        @FlowExp("List<String> list;") String s1;

        // :: error: (expression.unparsable.type.invalid)
        @FlowExp("s1 + s0") String s2;
    }
}
