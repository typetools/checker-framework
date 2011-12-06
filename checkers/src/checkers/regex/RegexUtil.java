package checkers.regex;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import checkers.nullness.quals.*;
import checkers.regex.quals.*;

/*
 * Utility methods for the Regex checker.
 */
public class RegexUtil {

  private RegexUtil()
  { throw new AssertionError("shouldn't be instantiated"); }

  /** 
   * Returns true if the argument is a syntactically valid regular
   * expression. 
   */
  @SuppressWarnings("regex")    // tests whether s is a regex
  public static boolean isRegex(String s) {
    try {
      Pattern.compile(s);
    } catch (PatternSyntaxException e) {
      return false;
    }
    return true;
  }

  /**
   * Returns null if the argument is a syntactically valid regular
   * expression. Otherwise returns a string describing why the string is
   * not a regex.
   */
  @SuppressWarnings("regex")    // tests whether s is a regex
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
  @SuppressWarnings("regex")    // tests whether s is a regex
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
  @SuppressWarnings("regex")    // suppresses warnings
  public static /*@Regex*/ String asRegex(String s) {
    try {
      Pattern.compile(s);
      return s;
    } catch (PatternSyntaxException e) {
      throw new Error(e);
    }
  }
}
