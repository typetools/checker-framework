// A test for the @MatchesRegex annotation.

import org.checkerframework.common.value.qual.*;

class RegexPatternSyntaxException {
    void stringConstants() {
        @MatchesRegex("(a*") String a = "a";
        // TODO: Add more tests like the above.
    }
}
