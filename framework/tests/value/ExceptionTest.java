import org.checkerframework.common.value.qual.*;

class ExceptionTest {

    public void foo() {
        int a = 5;
        String s = "hello";
        // :: warning: (method.evaluation.exception)
        char c = s.charAt(a);
    }
}
