// Test case for Issue 3281:
// https://github.com/typetools/checker-framework/issues/3281

import java.util.regex.Pattern;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.checker.regex.util.RegexUtil;

public class Issue3281 {

  @Regex String f = null;

  public boolean b = false;

  void m1(String s) {
    if (true) {
      // :: error: (argument)
      Pattern.compile(s);
    }
  }

  void m2(String s) {
    RegexUtil.isRegex(s);
    if (true) {
      // :: error: (argument)
      Pattern.compile(s);
    }
  }

  void m2f(String s) {
    RegexUtil.isRegex(s);
    if (true) {
      // :: error: (assignment)
      f = s;
    }
  }

  void m3(String s) {
    if (RegexUtil.isRegex(s)) {
      Pattern.compile(s);
    }
  }

  void m4(String s, String s2) {
    RegexUtil.isRegex(s);
    if (RegexUtil.isRegex(s2)) {
      // :: error: (argument)
      Pattern.compile(s);
    }
  }

  void m4f(String s, String s2) {
    RegexUtil.isRegex(s);
    if (RegexUtil.isRegex(s2)) {
      // :: error: (assignment)
      f = s;
    }
  }

  void m5f(String s, String s2) {
    RegexUtil.isRegex(s);
    if (b) {
      // :: error: (assignment)
      f = s;
    }
  }

  void foo(String s1, String s2) {
    bar(
        RegexUtil.isRegex(s1),
        // :: error: (argument)
        Pattern.compile(s1));
    boolean b;
    bar(
        b = RegexUtil.isRegex(s2),
        // :: error: (argument)
        Pattern.compile(s2));
  }

  void bar(boolean b, Object o) {}
}
