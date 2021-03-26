// Test case for Issue panacekcz#8:
// https://github.com/panacekcz/checker-framework/issues/8

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.index.qual.NonNegative;

public class RegexMatcher {
  static void m(String p, String s) {
    Matcher matcher = Pattern.compile(p).matcher(s);
    // The following line cannot be used as a test,
    // because the relation of matcher to p is not tracked,
    // so the upper bound is not known.

    // s.substring(matcher.start(), matcher.end());

    @NonNegative int i;
    i = matcher.start();
    i = matcher.end();
    // :: error: (assignment.type.incompatible)
    i = matcher.start(1);
    // :: error: (assignment.type.incompatible)
    i = matcher.end(1);
  }
}
