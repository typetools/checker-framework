import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.checker.regex.util.RegexUtil;

public class Test {
    void foo(String simple) {
        if (RegexUtil.isRegex(simple)) {
            @Regex String in = simple;
        }
    }
}
