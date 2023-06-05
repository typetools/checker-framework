import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class StringConcatenationTest {

  private String options_str;
  private String options_str2;
  private String options_str3;

  void foo() {
    options_str = getAinferSibling1();

    // Addition lubs the results, so these have no effect on the (default) type of the fields.
    // Also, the following two lines should behave identically.
    options_str2 += getAinferSibling1();
    options_str3 = options_str3 + getAinferSibling1();
  }

  void test() {
    // :: warning: (argument)
    expectsAinferSibling1(options_str);
  }

  @SuppressWarnings("argument")
  void test2() {
    expectsAinferSibling1(options_str2);
    expectsAinferSibling1(options_str3);
  }

  void expectsAinferSibling1(@AinferSibling1 String t) {}

  @SuppressWarnings("cast.unsafe")
  @AinferSibling1 String getAinferSibling1() {
    return (@AinferSibling1 String) " ";
  }
}
