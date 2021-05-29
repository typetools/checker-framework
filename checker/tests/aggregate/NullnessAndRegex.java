// Make sure that we actually receive errors from sub-checkers.

import org.checkerframework.checker.i18n.qual.Localized;
import org.checkerframework.checker.regex.qual.Regex;

public class NullnessAndRegex {
  // :: error: (assignment)
  @Regex String s1 = "De(mo";
  // :: error: (assignment)
  Object f = null;
  // :: error: (assignment)
  @Regex String s2 = "De(mo";

  void localized(@Localized String s) {}

  void method() {
    // :: error: (argument)
    localized("ldskjfldj"); // error
  }
}
