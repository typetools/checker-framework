import org.checkerframework.checker.regex.RegexUtil;
import org.checkerframework.checker.regex.qual.Regex;

public class Test {
    void foo(String simple) {
        if (RegexUtil.isRegex(simple)) {
            @Regex String in = simple;
        }
    }
}
