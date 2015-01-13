// Test case for Issue 291
// https://code.google.com/p/checker-framework/issues/detail?id=291
// @skip-test
import java.util.regex.*;
import org.checkerframework.checker.regex.RegexUtil;

public class MatcherGroupCount {
    public static void main(String[] args) {
        String regex = args[0];
        String content = args[1];

        if (!RegexUtil.isRegex(regex)) {
            System.out.println("Error parsing regex \"" + regex + "\": " +
                RegexUtil.regexException(regex).getMessage());
            System.exit(1);
        }

        Pattern pat = Pattern.compile(regex);
        Matcher mat = pat.matcher(content);

        if (mat.matches()) {
            if (mat.groupCount() > 0) {
                System.out.println("Group: " + mat.group(1));
            } else {
                System.out.println("No group found!");
            }
        } else {
            System.out.println("No match!");
        }
    }
}
