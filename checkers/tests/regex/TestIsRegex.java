import java.util.regex.Pattern;
import checkers.regex.RegexUtil;
import checkers.regex.quals.*;

class TestIsRegex {
    void test1(String str1) throws Exception {
        if (!RegexUtil.isRegex(str1)) {
            throw new Exception();
        }
        Pattern.compile(str1);
    }

    void test2(String str2) throws Exception {
        if (!RegexUtil.isRegex(str2)) {
            //:: error: (argument.type.incompatible)
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
            //:: error: (argument.type.incompatible)
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
            //:: error: (argument.type.incompatible)
            Pattern.compile(str6);
        }
    }

    void test7(String str7) throws Exception {
        if (RegexUtil.isRegex(str7, 5)) {
            //:: error: (group.count.invalid)
            Pattern.compile(str7).matcher("4kdfj").group(6);
        }
    }

    @Regex Pattern test8(String input) {
        String datePattern = null;

        if (input != null) {
          datePattern = "regexkdafj";
          if (!RegexUtil.isRegex(datePattern, 1)) {
            throw new Error("error parsing regex " + datePattern + ": "
                + RegexUtil.regexError(datePattern));
          }
          return Pattern.compile(datePattern);
        }
        @Regex(1) String dp = datePattern;

        if (input!=null) { // just some test...
            Pattern pattern = datePattern != null ? Pattern.compile(datePattern) : null;
            return pattern;
        } else {
            Pattern pattern = datePattern != null ? Pattern.compile(dp) : null;
            return pattern;
        }
    }

/* TODO: Why does the following, exact copy of test8 fail?
    @Regex(1) Pattern test9(String input) {
        String datePattern = null;

        if (input != null) {
          datePattern = "regexkdafj";
          if (!RegexUtil.isRegex(datePattern, 1)) {
            throw new Error("error parsing regex " + datePattern + ": "
                + RegexUtil.regexError(datePattern));
          }
          return Pattern.compile(datePattern);
        }
        @Regex(1) String dp = datePattern;

        if (input!=null) { // just some test...
            Pattern pattern = datePattern != null ? Pattern.compile(datePattern) : null;
            return pattern;
        } else {
            Pattern pattern = datePattern != null ? Pattern.compile(dp) : null;
            return pattern;
        }
      }*/
}

