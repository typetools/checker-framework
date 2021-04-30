import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

public class RegexUtilClient {
  void fullyQualifiedRegexUtil(String s) {
    if (org.checkerframework.checker.regex.util.RegexUtil.isRegex(s, 2)) {
      @Regex(2) String s2 = s;
    }
    @Regex(2) String s2 = org.checkerframework.checker.regex.util.RegexUtil.asRegex(s, 2);
  }

  void unqualifiedRegexUtil(String s) {
    if (RegexUtil.isRegex(s, 2)) {
      @Regex(2) String s2 = s;
    }
    @Regex(2) String s2 = RegexUtil.asRegex(s, 2);
  }

  void fullyQualifiedRegexUtilNoParamsArg(String s) {
    if (org.checkerframework.checker.regex.util.RegexUtil.isRegex(s)) {
      @Regex String s2 = s;
      @Regex(0) String s3 = s;
    }
    @Regex String s2 = org.checkerframework.checker.regex.util.RegexUtil.asRegex(s);
    @Regex(0) String s3 = org.checkerframework.checker.regex.util.RegexUtil.asRegex(s);
  }

  void unqualifiedRegexUtilNoParamsArg(String s) {
    if (RegexUtil.isRegex(s)) {
      @Regex String s2 = s;
      @Regex(0) String s3 = s;
    }
    @Regex String s2 = RegexUtil.asRegex(s, 2);
    @Regex(0) String s3 = RegexUtil.asRegex(s, 2);
  }

  void illegalName(String s) {
    if (IllegalName.isRegex(s, 2)) {
      // :: error: (assignment)
      @Regex(2) String s2 = s;
    }
    // :: error: (assignment)
    @Regex(2) String s2 = IllegalName.asRegex(s, 2);
  }

  void illegalNameRegexUtil(String s) {
    if (IllegalNameRegexUtil.isRegex(s, 2)) {
      // :: error: (assignment)
      @Regex(2) String s2 = s;
    }
    // :: error: (assignment)
    @Regex(2) String s2 = IllegalNameRegexUtil.asRegex(s, 2);
  }
}

// A dummy RegexUtil class to make sure RegexUtil in no package works.
class RegexUtil {
  @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
  public static boolean isRegex(final String s, int n) {
    return false;
  }

  public static @Regex String asRegex(String s, int n) {
    return null;
  }

  @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
  public static boolean isRegex(final String s) {
    return false;
  }

  public static @Regex String asRegex(String s) {
    return null;
  }
}

// These methods shouldn't work.
class IllegalName {
  public static boolean isRegex(String s, int n) {
    return false;
  }

  public static @Regex String asRegex(String s, int n) {
    return null;
  }
}

// These methods shouldn't work.
class IllegalNameRegexUtil {
  public static boolean isRegex(String s, int n) {
    return false;
  }

  public static @Regex String asRegex(String s, int n) {
    return null;
  }
}
