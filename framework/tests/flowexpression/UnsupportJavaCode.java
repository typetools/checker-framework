package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class UnsupportJavaCode {

    void method() {

        // :: error: (expression.unparsable.type.invalid)
        @FlowExp("new Object()") String s0;

        // :: error: (expression.unparsable.type.invalid)
        @FlowExp("List<String> list;") String s1;
    }
}
