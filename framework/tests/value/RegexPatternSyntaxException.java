// A test for malformed @MatchesRegex annotations.

import org.checkerframework.common.value.qual.*;

class RegexPatternSyntaxException {
    // :: warning: invalid.matches.regex
    void stringConstants(@MatchesRegex("(a*") String a) {
        // TODO: Add more tests like the above.
    }
}
