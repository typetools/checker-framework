import org.checkerframework.common.value.qual.*;

class ExceptionTest {

    public void foo() {
        int indexTooBig = 5;
        String s = "hello";
        // :: warning: (method.evaluation.exception)
        char c = s.charAt(indexTooBig);
    }
}
