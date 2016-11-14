package flowexpression;

import testlib.flowexpression.qual.FlowExp;

public class SimpleVPA {

    class MyClass {
        @FlowExp("this.bad")
        //:: error: (expression.unparsable.type.invalid)
        Object field;
    }

    class Use {
        Object bad = new Object();
        MyClass myClass = new MyClass();

        @FlowExp("bad")
        //:: error: (assignment.type.incompatible)
        Object o = myClass.field;
    }
}
