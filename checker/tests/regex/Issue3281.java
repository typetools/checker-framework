// Test case for Issue 3281:
// https://github.com/typetools/checker-framework/issues/3281

// @skip-test until the bug is fixed

import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.checker.regex.util.RegexUtil;

public class Issue3281 {

  @Regex String f = null;

  public boolean b = false;

  void m1(String s) {
    if (true) {
      // :: error: (argument.type.incompatible)
      Pattern.compile(s);
    }
  }

  void m2(String s) {
    RegexUtil.isRegex(s);
    if (true) {
      // :: error: (argument.type.incompatible)
      Pattern.compile(s);
    }
  }

  void m2f(String s) {
    RegexUtil.isRegex(s);
    if (true) {
      // :: error: (assignment.type.incompatible)
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
      Pattern.compile(s);
    }
  }

  void m4f(String s, String s2) {
    RegexUtil.isRegex(s);
    if (RegexUtil.isRegex(s2)) {
      // :: error: (assignment.type.incompatible)
      f = s;
    }
  }

  void m5f(String s, String s2) {
    RegexUtil.isRegex(s);
    if (b) {
      // :: error: (assignment.type.incompatible)
      f = s;
    }
  }
}
