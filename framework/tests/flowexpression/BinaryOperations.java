package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class BinaryOperations {

    void method(int i, int j, @FlowExp("#1+#2") String s) {
        @FlowExp("i+j") String q = s;
    }
}
