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

  // These methods should be kept in sync with those in plume.UtilMDE .

  /** 
   * Returns true if the argument is a syntactically valid regular
   * expression. 
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static boolean isRegex(String s) {
    try {
      Pattern.compile(s);
    } catch (PatternSyntaxException e) {
      return false;
    }
    return true;
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
   * expression. Otherwise returns a string describing why the string is
   * not a regex.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static /*@Nullable*/ String regexError(String s) {
    try {
      Pattern.compile(s);
    } catch (PatternSyntaxException e) {
      return e.getMessage();
    }
    return null;
  }
  
  /**
   * Returns null if the argument is a syntactically valid regular
   * expression. Otherwise returns a PatternSyntaxException describing
   * why the string is not a regex.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static /*@Nullable*/ PatternSyntaxException regexException(String s) {
    try {
      Pattern.compile(s);
    } catch (PatternSyntaxException pse) {
      return pse;
    }
    return null;
  }

  /**
   * Returns the argument if it is a regex, otherwise throws an error.
   * The purpose of this method is to suppress Regex Checker warnings.
   * Once the the Regex Checker supports flow-sensitivity, it should be
   * very rarely needed.
   */
  @SuppressWarnings("regex")    // RegexUtil
  /*@Pure*/
  public static /*@Regex*/ String asRegex(String s) {
    try {
      Pattern.compile(s);
      return s;
    } catch (PatternSyntaxException e) {
      throw new Error(e);
    }
  }
}
