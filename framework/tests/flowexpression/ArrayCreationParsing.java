package flowexpression;

import testlib.flowexpression.qual.FlowExp;

public class ArrayCreationParsing {
    @FlowExp("new int[2]") Object value1;

    @FlowExp("new int[2][2]") Object value2;

    @FlowExp("new String[2]") Object value3;

    @FlowExp("new String[] {\"a\", \"b\"}") Object value4;

    void method(@FlowExp("new java.lang.String[2]") Object param) {
        value3 = param;
        // :: error: (assignment.type.incompatible)
        value1 = param;
    }
}
