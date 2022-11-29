import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class StringConcatenationTest {

  private String options_str;
  private String options_str2;

  void foo() {
    options_str = getAinferSibling1();
    options_str2 += getAinferSibling1();
  }

  void test() {
    // :: warning: (argument)
    expectsAinferSibling1(options_str);
    // :: warning: (argument)
    expectsAinferSibling1(options_str2);
  }

  void expectsAinferSibling1(@AinferSibling1 String t) {}

  @SuppressWarnings("cast.unsafe")
  @AinferSibling1 String getAinferSibling1() {
    return (@AinferSibling1 String) " ";
  }
}
