import tests.signatureinference.qual.Top;
import tests.signatureinference.qual.SignatureInferenceBottom;
import tests.signatureinference.qual.*;
public class ParameterInferenceTest {

    void test1() {
        @SignatureInferenceBottom int bot = (@SignatureInferenceBottom int) 0;
        expectsBotNoSignature(bot);
    }

    void expectsBotNoSignature(@SignatureInferenceBottom int t) {
        @SignatureInferenceBottom int bot = t;
    }

    void test2() {
        @Top int top = (@Top int) 0;
        expectsTopNoSignature(top);
    }

    void expectsTopNoSignature(@Top int t) {}

}
