import java.util.regex.Pattern;
import checkers.regex.RegexUtil;

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
}

