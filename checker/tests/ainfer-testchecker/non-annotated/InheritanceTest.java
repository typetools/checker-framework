import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;

class IParent {
    int field;

    public void expectsBotNoSignature(int t) {
        // :: warning: (argument.type.incompatible)
        expectsBot(t);
        // :: warning: (argument.type.incompatible)
        expectsBot(field);
    }

    void expectsBot(@AinferBottom int t) {}
}

class IChild extends IParent {
    void test1() {
        @AinferBottom int bot = (@AinferBottom int) 0;
        expectsBotNoSignature(bot);
        field = bot;
    }
}
