import testlib.flowexpression.qual.FlowExp;

public class ViewpointAdaptation {

    class MyClass {
        protected final Object field = new Object();

        protected @FlowExp("field") Object annotatedField1;

        protected @FlowExp("this.field") Object annotatedField2;

        public @FlowExp("field") Object getAnnotatedField1() {
            return annotatedField1;
        }
    }

    class Use {
        final MyClass myClass1 = new MyClass();
        final Object field = new Object();

        @FlowExp("this.myClass1.field")
        Object o1 = myClass1.annotatedField1;

        @FlowExp("this.myClass1.field")
        Object o2 = myClass1.annotatedField2;

        @FlowExp("field")
        //:: error: (assignment.type.incompatible)
        Object o3 = myClass1.annotatedField1;

        @FlowExp("this.field")
        //:: error: (assignment.type.incompatible)
        Object o4 = myClass1.annotatedField2;

        @FlowExp("field")
        //:: error: (assignment.type.incompatible)
        Object oM2 = myClass1.getAnnotatedField1();

        @FlowExp("this.field")
        //:: error: (assignment.type.incompatible)
        Object oM3 = myClass1.getAnnotatedField1();
    }
}
