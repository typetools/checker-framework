// A test for malformed @MatchesRegex annotations.

import org.checkerframework.common.value.qual.*;

class RegexPatternSyntaxException {
    // :: warning: invalid.matches.regex
    void stringConstants(@MatchesRegex("(a*") String a) {}
}
