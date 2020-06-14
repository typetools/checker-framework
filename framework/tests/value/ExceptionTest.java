import org.checkerframework.common.value.qual.*;

class ExceptionTest {

    public void foo() {
        String s = "hello";

        int indexOk = 1;
        @IntVal('e') char c1 = s.charAt(indexOk);

        int indexTooBig = 5;
        // :: warning: (method.evaluation.exception)
        char c2 = s.charAt(indexTooBig);
    }
}
