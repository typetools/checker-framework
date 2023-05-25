// A test for malformed @MatchesRegex and @DoesNotMatchRegex annotations.

import org.checkerframework.common.value.qual.*;

public class RegexPatternSyntaxException {
  // :: warning: (invalid.matches.regex)
  void stringConstants1(@MatchesRegex("(a*") String a) {}

  // :: warning: (invalid.doesnotmatch.regex)
  void stringConstants2(@DoesNotMatchRegex("(a*") String a) {}
}
