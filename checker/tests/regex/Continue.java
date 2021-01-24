import java.util.regex.Pattern;
import org.checkerframework.checker.regex.util.RegexUtil;

public class Continue {

    void test1(String[] a) {
        for (String s : a) {
            if (!RegexUtil.isRegex(s)) {
                continue;
            }
            Pattern.compile(s);
        }
    }

    void test2(String[] a, boolean b) {
        for (String s : a) {
            if (!RegexUtil.isRegex(s)) {
                continue;
            } else if (b) {
                continue;
            }
            Pattern.compile(s);
        }
    }

    // Reverse the if statements from the previous test.
    void test3(String[] a, boolean b) {
        for (String s : a) {
            if (b) {
                continue;
            } else if (!RegexUtil.isRegex(s)) {
                continue;
            }
            Pattern.compile(s);
        }
    }

    void twoThrows(String s) {
        if (s == null) {
            throw new RuntimeException();
        } else if (!RegexUtil.isRegex(s)) {
            throw new RuntimeException();
        }
        Pattern.compile(s);
    }

    void twoReturns(String s) {
        if (s == null) {
            return;
        } else if (!RegexUtil.isRegex(s)) {
            return;
        }
        Pattern.compile(s);
    }
}
