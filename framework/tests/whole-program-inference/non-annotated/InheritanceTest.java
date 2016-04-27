//@skip-test
// TODO: Enable this test when whole-program inference is working correctly for cases
// involving inheritance.
import tests.wholeprograminference.qual.*;
class Parent {
    int field;

    void expectsBotNoSignature(int t) {
        //:: error: (argument.type.incompatible)
        expectsBot(t);
    }

    void expectsBot(@WholeProgramInferenceBottom int t) {}

    void test() {
        expectsBotNoSignature(field);
    }

}

class Child extends Parent {
    void test1() {
        @WholeProgramInferenceBottom int bot = (@WholeProgramInferenceBottom int) 0;
        expectsBotNoSignature(bot);
        field = bot;
    }

}
