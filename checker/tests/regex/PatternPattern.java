// Test case for https://github.com/typetools/checker-framework/issues/7539

import java.util.regex.Pattern;

public class PatternPattern {
  public static String f() {
    Pattern abc = Pattern.compile("abc");
    String str = abc.pattern();
    return "abc".replaceAll(str, "");
  }
}
