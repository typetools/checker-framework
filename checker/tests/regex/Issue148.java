// test-case for issue 148

import org.checkerframework.checker.regex.qual.*;

class Issue148 {
    public static void main(String[] args) {
        if (!org.checkerframework.checker.regex.RegexUtil.isRegex(args[0], 4)) {
            return;
        }
        @Regex(4) String regex = args[0];
    }
}
