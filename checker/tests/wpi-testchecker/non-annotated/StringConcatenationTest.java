import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;

public class StringConcatenationTest {

  private String options_str;
  private String options_str2;

  void foo() {
    options_str = getSibling1();
    options_str2 += getSibling1();
  }

  void test() {
    // :: warning: (argument)
    expectsSibling1(options_str);
    // :: warning: (argument)
    expectsSibling1(options_str2);
  }

  void expectsSibling1(@Sibling1 String t) {}

  @SuppressWarnings("cast.unsafe")
  @Sibling1 String getSibling1() {
    return (@Sibling1 String) " ";
  }
}
