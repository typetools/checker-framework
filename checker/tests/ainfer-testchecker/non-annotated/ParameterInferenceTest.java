import org.checkerframework.checker.testchecker.ainfer.qual.Parent;
import org.checkerframework.checker.testchecker.ainfer.qual.Top;

public class ParameterInferenceTest {

    void test1() {
        @Parent int parent = (@Parent int) 0;
        expectsParentNoSignature(parent);
    }

    void expectsParentNoSignature(int t) {
        // :: warning: (assignment.type.incompatible)
        @Parent int parent = t;
    }

    void test2() {
        @Top int top = (@Top int) 0;
        // :: warning: (argument.type.incompatible)
        expectsTopNoSignature(top);
    }

    void expectsTopNoSignature(int t) {}
}
