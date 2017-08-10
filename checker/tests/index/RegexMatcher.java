// Test case for Issue panacekcz#8:
// https://github.com/panacekcz/checker-framework/issues/8

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMatcher {
    static void m(String p, String s) {
        Matcher matcher = Pattern.compile(p).matcher(s);

        s.substring(matcher.start(), matcher.end());
        //:: error: (argument.type.incompatible)
        s.substring(matcher.start(1), matcher.end(1));
    }
}
