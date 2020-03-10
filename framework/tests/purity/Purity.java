import org.checkerframework.dataflow.qual.*;

interface PureFunc {
    @Pure
    String doNothing();
}

class TestPure {

    static String myMethod() {
        return "";
    }

    @Pure
    static String myPureMethod() {
        return "";
    }

    void context() {
        PureFunc f1 = TestPure::myPureMethod;
        // :: error: (purity.invalid.methodref)
        PureFunc f2 = TestPure::myMethod;
    }
}
