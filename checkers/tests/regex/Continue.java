import checkers.regex.RegexUtil;
import java.util.regex.Pattern;

class Continue {

  void test1(String[] a) {
    for (String s : a) {
      if (!RegexUtil.isRegex(s)) {
        continue;
      }
      Pattern.compile(s);
    }
  }

  // TODO: enable the following tests.
  // There's a bug in flow for an if and an else if that both have continues or throws. 
  @skip-test
  void test2(String[] a, boolean b) {
    for (String s : a) {
      if (!RegexUtil.isRegex(s)) {
        continue;
      } else if (b) {
        continue;
      }
      Pattern.compile(s);
    }
  }

  // Reverse the if statements from the previous test.
  @skip-test
  void test3(String[] a, boolean b) {
    for (String s : a) {
      if (b) {
        continue;
      } else if (!RegexUtil.isRegex(s)) {
        continue;
      }
      Pattern.compile(s);
    }
  }

  @skip-test
  void twoThrows(String s) {
    if (s == null) {
      throw new RuntimeException();
    } else if (!RegexUtil.isRegex(s)) {
      throw new RuntimeException();
    }
    Pattern.compile(s);
  }
}
