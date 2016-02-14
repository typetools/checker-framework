//@skip-test
// TODO: Enable this test when signature inference is working correctly for cases
// involving inheritance.
import tests.signatureinference.qual.*;
class Parent {
    int field;

    void expectsBotNoSignature(int t) {
        //:: error: (argument.type.incompatible)
        expectsBot(t);
    }

    void expectsBot(@SignatureInferenceBottom int t) {}

    void test() {
        expectsBotNoSignature(field);
    }

}

class Child extends Parent {
    void test1() {
        @SignatureInferenceBottom int bot = (@SignatureInferenceBottom int) 0;
        expectsBotNoSignature(bot);
        field = bot;
    }

}
