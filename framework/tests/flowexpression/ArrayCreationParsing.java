package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class ArrayCreationParsing {
    @FlowExp("new int[2]") Object value1;

    @FlowExp("new int[2][2]") Object value2;

    @FlowExp("new String[2]") Object value3;

    @FlowExp("new String[] {\"a\", \"b\"}")
    Object value4;

    int i;

    @FlowExp("new int[i]") Object value5;

    @FlowExp("new int[this.i]") Object value6;

    @FlowExp("new int[getI()]") Object value7;

    @FlowExp("new int[] {i, this.i, getI()}") Object value8;

    int getI() {
        return i;
    }

    void method(@FlowExp("new java.lang.String[2]") Object param) {
        value3 = param;
        // :: error: (assignment.type.incompatible)
        value1 = param;
    }
}
