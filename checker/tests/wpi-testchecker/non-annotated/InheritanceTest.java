import org.checkerframework.checker.testchecker.wholeprograminference.qual.WholeProgramInferenceBottom;

class IParent {
  int field;

  public void expectsBotNoSignature(int t) {
    // :: warning: (argument)
    expectsBot(t);
    // :: warning: (argument)
    expectsBot(field);
  }

  void expectsBot(@WholeProgramInferenceBottom int t) {}
}

class IChild extends IParent {
  void test1() {
    @WholeProgramInferenceBottom int bot = (@WholeProgramInferenceBottom int) 0;
    expectsBotNoSignature(bot);
    field = bot;
  }
}
