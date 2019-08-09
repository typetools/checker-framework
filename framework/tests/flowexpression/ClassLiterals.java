package flowexpression;

import testlib.flowexpression.qual.FlowExp;

public class ClassLiterals {
    static class String {}

    void method(
            @FlowExp("String.class") Object p1,
            @FlowExp("String.class") Object p2,
            @FlowExp("java.lang.String.class") Object p3) {
        @FlowExp("String.class") Object l1 = p1;
        @FlowExp("String.class") Object l2 = p2;
        // :: error: (assignment.type.incompatible)
        @FlowExp("String.class") Object l3 = p3;
        // :: error: (assignment.type.incompatible)
        @FlowExp("java.lang.String.class") Object l4 = p1;
        // :: error: (assignment.type.incompatible)
        @FlowExp("java.lang.String.class") Object l5 = p2;
        @FlowExp("java.lang.String.class") Object l6 = p3;
    }

    // :: error: (expression.unparsable.type.invalid)
    @FlowExp("int.class") String s0;

    // :: error: (expression.unparsable.type.invalid)
    @FlowExp("int[].class") String s1;

    // :: error: (expression.unparsable.type.invalid)
    @FlowExp("String[].class") String s2;

    // :: error: (expression.unparsable.type.invalid)
    @FlowExp("java.lang.String[].class") String s3;
}
