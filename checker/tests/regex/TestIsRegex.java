import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.checker.regex.util.RegexUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestIsRegex {
    void test1(String str1) throws Exception {
        if (!RegexUtil.isRegex(str1)) {
            throw new Exception();
        }
        Pattern.compile(str1);
    }

    void test2(String str2) throws Exception {
        if (!RegexUtil.isRegex(str2)) {
            // :: error: (argument.type.incompatible)
            Pattern.compile(str2);
        }
    }

    void test3(String str3) throws Exception {
        if (RegexUtil.isRegex(str3)) {
            Pattern.compile(str3);
        } else {
            throw new Exception();
        }
    }

    void test4(String str4) throws Exception {
        if (RegexUtil.isRegex(str4)) {
            Pattern.compile(str4);
        } else {
            // :: error: (argument.type.incompatible)
            Pattern.compile(str4);
        }
    }

    void test5(String str5) throws Exception {
        if (!RegexUtil.isRegex(str5, 3)) {
            throw new Exception();
        }
        Pattern.compile(str5).matcher("test").group(3);
    }

    void test6(String str6) throws Exception {
        if (RegexUtil.isRegex(str6, 4)) {
            Pattern.compile(str6).matcher("4kdfj").group(4);
        } else {
            // :: error: (argument.type.incompatible)
            Pattern.compile(str6);
        }
    }

    void test7(String str7) throws Exception {
        if (RegexUtil.isRegex(str7, 5)) {
            // :: error: (group.count.invalid)
            Pattern.compile(str7).matcher("4kdfj").group(6);
        }
    }

    @Regex Pattern test8(String input) {
        String datePattern = null;

        if (input != null) {
            datePattern = "regexkdafj";
            if (!RegexUtil.isRegex(datePattern, 1)) {
                throw new Error(
                        "error parsing regex "
                                + datePattern
                                + ": "
                                + RegexUtil.regexError(datePattern));
            }
            return Pattern.compile(datePattern);
        }
        @Regex(1) String dp = datePattern;

        if (input != null) { // just some test...
            Pattern pattern = datePattern != null ? Pattern.compile(datePattern) : null;
            return pattern;
        } else {
            Pattern pattern = datePattern != null ? Pattern.compile(dp) : null;
            return pattern;
        }
    }

    @Regex(1) Pattern test9(String input) {
        String datePattern = null;

        if (input != null) {
            datePattern = "regexkdafj";
            if (!RegexUtil.isRegex(datePattern, 1)) {
                throw new Error(
                        "error parsing regex "
                                + datePattern
                                + ": "
                                + RegexUtil.regexError(datePattern));
            }
            return Pattern.compile(datePattern);
        }
        @Regex(1) String dp = datePattern;

        if (input != null) { // just some test...
            Pattern pattern = datePattern != null ? Pattern.compile(datePattern) : null;
            return pattern;
        } else {
            Pattern pattern = datePattern != null ? Pattern.compile(dp) : null;
            return pattern;
        }
    }

    void test10(String s) throws Exception {
        if (!RegexUtil.isRegex(s, 2)) {
            throw new Exception();
        }
        Pattern p = Pattern.compile(s);
        Matcher m = p.matcher("abc");
        String g = m.group(1);
    }

    void test11(String s) throws Exception {
        @Regex(2) String l1 = RegexUtil.asRegex(s, 2);
        @Regex(1) String l2 = RegexUtil.asRegex(s, 2);
        @Regex String l3 = RegexUtil.asRegex(s, 2);
        // :: error: (assignment.type.incompatible)
        @Regex(3) String l4 = RegexUtil.asRegex(s, 2);
    }

    @Regex(2) String test12(String s, boolean b) throws Exception {
        return b ? null : RegexUtil.asRegex(s, 2);
    }
}
