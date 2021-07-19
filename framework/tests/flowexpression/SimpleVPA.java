package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class SimpleVPA {

    class MyClass {
        // :: error: (expression.unparsable.type.invalid)
        @FlowExp("this.bad") Object field;
    }

    class Use {
        Object bad = new Object();
        MyClass myClass = new MyClass();

        @FlowExp("bad")
        // :: error: (assignment.type.incompatible)
        Object o = myClass.field;
    }
}
