// Test case for issue #3267:
// https://github.com/typetools/checker-framework/issues/3267

import org.checkerframework.checker.regex.util.RegexUtil;

import java.util.regex.Pattern;

public class Issue3267 {
    void foo(String s) {
        if (RegexUtil.isRegex(s)) {
        } else {
        }
        if (true) {
            // :: error: (argument.type.incompatible)
            Pattern.compile(s);
        }
    }
}
