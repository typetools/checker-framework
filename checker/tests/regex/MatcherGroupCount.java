// Test case for Issue 291
// https://github.com/typetools/checker-framework/issues/291

import java.util.regex.*;
import org.checkerframework.checker.regex.util.RegexUtil;

public class MatcherGroupCount {
  public static void main(String[] args) {
    String regex = args[0];
    String content = args[1];

    if (!RegexUtil.isRegex(regex)) {
      System.out.println(
          "Error parsing regex \"" + regex + "\": " + RegexUtil.regexException(regex).getMessage());
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
      if (mat.groupCount() >= 2) {
        System.out.println("Group: " + mat.group(2));
      }
      if (mat.groupCount() >= 2) {
        // :: error: (group.count)
        System.out.println("Group: " + mat.group(3));
      }
      if (2 < mat.groupCount()) {
        System.out.println("Group: " + mat.group(3));
      }
      if (!(mat.groupCount() > 4)) {
        System.out.println("Group: " + mat.group(0));
      } else {
        System.out.println("Group: " + mat.group(5));
        // :: error: (group.count)
        System.out.println("Group: " + mat.group(6));
      }
    } else {
      System.out.println("No match!");
    }
  }
}
