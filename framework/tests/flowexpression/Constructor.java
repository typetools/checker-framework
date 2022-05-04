import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class Constructor {

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    static class MyClass {
        String field;

        @FlowExp("field") MyClass() {}
    }

    static class MyClass2 {
        String field;
        String field2;

        @SuppressWarnings("cast.unsafe.constructor.invocation")
        void method() {
            // TODO: This should be an error.
            MyClass c = new @FlowExp("field") MyClass();
            // :: error: (expression.unparsable.type.invalid) :: error:
            // (constructor.invocation.invalid)
            MyClass c2 = new @FlowExp("bad") MyClass();
            // :: error: (constructor.invocation.invalid)
            MyClass c3 = new @FlowExp("field2") MyClass();
        }
    }
}
