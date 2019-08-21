package flowexpression;

import testlib.flowexpression.qual.FlowExp;

public class InnerClasses {
    public String outerInstanceField = "";
    public static String outerStaticField = "";

    static class InnerClass {
        // :: error: (expression.unparsable.type.invalid)
        @FlowExp("outerInstanceField") Object o = null;

        @FlowExp("outerStaticField") Object o2 = null;
    }

    class NonStaticInnerClass {
        @FlowExp("outerInstanceField") Object o = null;

        @FlowExp("outerStaticField") Object o2 = null;
    }

    static class InnerClass2 {
        public String outerInstanceField = "";

        @FlowExp("outerInstanceField") Object o = null;
    }

    class TestUses {
        void method(InnerClass innerClass, InnerClass2 innerClass2) {
            // :: error: (expression.unparsable.type.invalid) :: error:
            // (assignment.type.incompatible)
            @FlowExp("innerClass.outerInstanceField") Object o = innerClass.o;
            @FlowExp("InnerClasses.outerStaticField") Object o2 = innerClass.o2;

            @FlowExp("innerClass2.outerInstanceField") Object o3 = innerClass2.o;
        }
    }
}
