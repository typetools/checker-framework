import java.util.function.Function;
import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class LambdaParameter {

    void method(String methodParam) {
        Function<String, String> func1 =
                (
                        // :: error: (lambda.param.type.incompatible)
                        @FlowExp("methodParam") String lambdaParam) -> {
                    return "";
                };
        Function<String, String> func2 =
                (
                        // :: error: (lambda.param.type.incompatible) :: error:
                        // (expression.unparsable.type.invalid)
                        @FlowExp("lambdaParam") String lambdaParam) -> {
                    return "";
                };
        Function<String, String> func3 =
                (
                        // :: error: (lambda.param.type.incompatible)
                        @FlowExp("#1") String lambdaParam) -> {
                    @FlowExp("lambdaParam") String s = lambdaParam;
                    return "";
                };
        Function<@FlowExp("methodParam") String, String> func4 =
                (
                        @FlowExp("methodParam") String lambdaParam) -> {
                    return "";
                };
    }

    void method2(String methodParam, @FlowExp("#1") String methodParam2) {
        Function<@FlowExp("methodParam") String, String> func1 =
                (
                        @FlowExp("methodParam") String lambdaParam) -> {
                    @FlowExp("methodParam") String a = methodParam2;
                    @FlowExp("methodParam") String b = lambdaParam;
                    return "";
                };
    }

    void method3() {
        String local = "";
        Function<String, String> func1 =
                (
                        // :: error: (lambda.param.type.incompatible)
                        @FlowExp("local") String lambdaParam) -> {
                    return "";
                };
        Function<@FlowExp("local") String, String> func2 =
                (
                        @FlowExp("local") String lambdaParam) -> {
                    return "";
                };
    }

    void method4() {
        String local = "";
        @FlowExp("local") String otherLocal = null;
        Function<@FlowExp("local") String, String> func1 =
                (
                        @FlowExp("local") String lambdaParam) -> {
                    @FlowExp("local") String a = otherLocal;
                    @FlowExp("local") String b = lambdaParam;
                    return "";
                };
    }
}
