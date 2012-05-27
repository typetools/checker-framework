package checkers.regex;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import checkers.nullness.quals.*;
import checkers.regex.quals.*;

// This class should be kept in sync with plume.RegexUtil .

/**
 * Utility methods for regular expressions.
 * <p>
 *
 * For an example of intended use, see section <a
 * href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#regexutil-methods">Testing
 * whether a string is a regular expression</a> in the Checker Framework
 * manual.
 * <p>
 *
 * <b>Runtime Dependency</b>:
 * Using this class introduces a runtime dependency.
 * This means that you need to distribute (or link to) the Checker
 * Framework, along with your binaries.
 * To eliminate this dependency, you can simply copy this class into your
 * own project.
 */
public class RegexUtil {

  private RegexUtil() {
    throw new AssertionError("Class RegexUtil shouldn't be instantiated");
  }

  /**
   * Returns true if the argument is a syntactically valid regular
   * expression.
   */
  public static boolean isRegex(String s) {
    return isRegex(s, 0);
  }

  /**
   * Returns true if the argument is a syntactically valid regular
   * expression with at least the given number of groups.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static boolean isRegex(String s, int groups) {
    Pattern p;
    try {
      p = Pattern.compile(s);
    } catch (PatternSyntaxException e) {
      return false;
    }
    return getGroupCount(p) >= groups;
  }

  /**
   * Returns true if the argument is a syntactically valid regular
   * expression.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static boolean isRegex(char c) {
    return isRegex(Character.toString(c));
  }

  /**
   * Returns null if the argument is a syntactically valid regular
   * expression. Otherwise returns a string describing why the argument is
   * not a regex.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static /*@Nullable*/ String regexError(String s) {
    return regexError(s, 0);
  }

  /**
   * Returns null if the argument is a syntactically valid regular
   * expression with at least the given number of groups. Otherwise returns
   * a string describing why the argument is not a regex.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static /*@Nullable*/ String regexError(String s, int groups) {
    try {
      Pattern p = Pattern.compile(s);
      int actualGroups = getGroupCount(p);
      if (actualGroups < groups) {
        return regexErrorMessage(s, groups, actualGroups);
      }
    } catch (PatternSyntaxException e) {
      return e.getMessage();
    }
    return null;
  }

  /**
   * Returns null if the argument is a syntactically valid regular
   * expression. Otherwise returns a PatternSyntaxException describing
   * why the argument is not a regex.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static /*@Nullable*/ PatternSyntaxException regexException(String s) {
    return regexException(s, 0);
  }

  /**
   * Returns null if the argument is a syntactically valid regular
   * expression with at least the given number of groups. Otherwise returns a
   * PatternSyntaxException describing why the argument is not a regex.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static /*@Nullable*/ PatternSyntaxException regexException(String s, int groups) {
    try {
      Pattern p = Pattern.compile(s);
      int actualGroups = getGroupCount(p);
      if (actualGroups < groups) {
        return new PatternSyntaxException(regexErrorMessage(s, groups, actualGroups), s, -1);
      }
    } catch (PatternSyntaxException pse) {
      return pse;
    }
    return null;
  }

  /**
   * Returns the argument as a {@code @Regex String} if it is a regex,
   * otherwise throws an error. The purpose of this method is to suppress Regex
   * Checker warnings. Once the the Regex Checker supports flow-sensitivity, it
   * should be very rarely needed.
   */
  public static /*@Regex*/ String asRegex(String s) {
    return asRegex(s, 0);
  }

  /**
   * Returns the argument as a {@code @Regex(groups) String} if it is a regex
   * with at least the given number of groups, otherwise throws an error. The
   * purpose of this method is to suppress Regex Checker warnings. Once the the
   * Regex Checker supports flow-sensitivity, it should be very rarely needed.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static /*@Regex*/ String asRegex(String s, int groups) {
    try {
      Pattern p = Pattern.compile(s);
      int actualGroups = getGroupCount(p);
      if (actualGroups < groups) {
        throw new Error(regexErrorMessage(s, groups, actualGroups));
      }
      return s;
    } catch (PatternSyntaxException e) {
      throw new Error(e);
    }
  }

  /**
   * Generates an error message for s when expectedGroups are needed, but s
   * only has actualGroups.
   */
  private static String regexErrorMessage(String s, int expectedGroups, int actualGroups) {
    return "regex \"" + s + "\" has " + actualGroups + " groups, but " +
        expectedGroups + " groups are needed.";
  }

  /**
   * Returns the count of groups in the argument.
   */
  private static int getGroupCount(Pattern p) {
    return p.matcher("").groupCount();
  }
}
