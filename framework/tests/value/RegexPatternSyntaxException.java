// A test for malformed @MatchesRegex annotations.

import org.checkerframework.common.value.qual.*;

public class RegexPatternSyntaxException {
  // :: warning: invalid.matches.regex
  void stringConstants1(@MatchesRegex("(a*") String a) {}
}
