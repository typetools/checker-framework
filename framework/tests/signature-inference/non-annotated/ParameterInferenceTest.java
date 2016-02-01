import tests.signatureinference.qual.*;
public class ParameterInferenceTest {

    void test1() {
        @SignatureInferenceBottom int bot = (@SignatureInferenceBottom int) 0;
        expectsBotNoSignature(bot);
    }

    void expectsBotNoSignature(int t) {
        //:: error: (assignment.type.incompatible)
        @SignatureInferenceBottom int bot = t;
    }

    void test2() {
        @Top int top = (@Top int) 0;
        //:: error: (argument.type.incompatible)
        expectsTopNoSignature(top);
    }

    void expectsTopNoSignature(int t) {}

}
