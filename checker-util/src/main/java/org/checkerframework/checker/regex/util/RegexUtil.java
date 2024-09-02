// This class should be kept in sync with org.plumelib.util.RegexUtil in the plume-util project.
// (Warning suppressions may differ.)

package org.checkerframework.checker.regex.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

/**
 * Utility methods for regular expressions, most notably for testing whether a string is a regular
 * expression.
 *
 * <p>For an example of intended use, see section <a
 * href="https://checkerframework.org/manual/#regexutil-methods">Testing whether a string is a
 * regular expression</a> in the Checker Framework manual.
 *
 * <p><b>Runtime Dependency</b>: If you use this class, you must distribute (or link to) {@code
 * checker-qual.jar}, along with your binaries. Or, you can copy this class into your own project.
 */
@AnnotatedFor("nullness")
public final class RegexUtil {

  /** This class is a collection of methods; it does not represent anything. */
  private RegexUtil() {
    throw new Error("do not instantiate");
  }

  /**
   * A checked version of {@link PatternSyntaxException}.
   *
   * <p>This exception is useful when an illegal regex is detected but the contextual information to
   * report a helpful error message is not available at the current depth in the call stack. By
   * using a checked PatternSyntaxException the error must be handled up the call stack where a
   * better error message can be reported.
   *
   * <p>Typical usage is:
   *
   * <pre>
   * void myMethod(...) throws CheckedPatternSyntaxException {
   *   ...
   *   if (! isRegex(myString)) {
   *     throw new CheckedPatternSyntaxException(...);
   *   }
   *   ... Pattern.compile(myString) ...
   * </pre>
   *
   * Simply calling {@code Pattern.compile} would have a similar effect, in that {@code
   * PatternSyntaxException} would be thrown at run time if {@code myString} is not a regular
   * expression. There are two problems with such an approach. First, a client of {@code myMethod}
   * might forget to handle the exception, since {@code PatternSyntaxException} is not checked.
   * Also, the Regex Checker would issue a warning about the call to {@code Pattern.compile} that
   * might throw an exception. The above usage pattern avoids both problems.
   *
   * @see PatternSyntaxException
   */
  public static class CheckedPatternSyntaxException extends Exception {

    /** Unique identifier for serialization. If you add or remove fields, change this number. */
    private static final long serialVersionUID = 6266881831979001480L;

    /** The PatternSyntaxException that this is a wrapper around. */
    private final PatternSyntaxException pse;

    /**
     * Constructs a new CheckedPatternSyntaxException equivalent to the given {@link
     * PatternSyntaxException}.
     *
     * <p>Consider calling this constructor with the result of {@link RegexUtil#regexError}.
     *
     * @param pse the PatternSyntaxException to be wrapped
     */
    public CheckedPatternSyntaxException(PatternSyntaxException pse) {
      this.pse = pse;
    }

    /**
     * Constructs a new CheckedPatternSyntaxException.
     *
     * @param desc a description of the error
     * @param regex the erroneous pattern
     * @param index the approximate index in the pattern of the error, or {@code -1} if the index is
     *     not known
     */
    public CheckedPatternSyntaxException(String desc, String regex, @GTENegativeOne int index) {
      this(new PatternSyntaxException(desc, regex, index));
    }

    /**
     * Retrieves the description of the error.
     *
     * @return the description of the error
     */
    public String getDescription() {
      return pse.getDescription();
    }

    /**
     * Retrieves the error index.
     *
     * @return the approximate index in the pattern of the error, or {@code -1} if the index is not
     *     known
     */
    public int getIndex() {
      return pse.getIndex();
    }

    /**
     * Returns a multi-line string containing the description of the syntax error and its index, the
     * erroneous regular-expression pattern, and a visual indication of the error index within the
     * pattern.
     *
     * @return the full detail message
     */
    @Override
    @Pure
    public String getMessage(@GuardSatisfied CheckedPatternSyntaxException this) {
      return pse.getMessage();
    }

    /**
     * Retrieves the erroneous regular-expression pattern.
     *
     * @return the erroneous pattern
     */
    public String getPattern() {
      return pse.getPattern();
    }
  }

  /**
   * Returns true if the argument is a syntactically valid regular expression.
   *
   * @param s string to check for being a regular expression
   * @return true iff s is a regular expression
   */
  @Pure
  @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
  public static boolean isRegex(String s) {
    return isRegex(s, 0);
  }

  /**
   * Returns true if the argument is a syntactically valid regular expression with at least the
   * given number of groups.
   *
   * @param s string to check for being a regular expression
   * @param groups number of groups expected
   * @return true iff s is a regular expression with {@code groups} groups
   */
  @SuppressWarnings("regex") // RegexUtil
  @Pure
  // @EnsuresQualifierIf annotation is extraneous because this method is special-cased
  // in RegexTransfer.
  @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
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
   * Returns true if the argument is a syntactically valid regular expression.
   *
   * @param c char to check for being a regular expression
   * @return true iff c is a regular expression
   */
  @SuppressWarnings({
    "regex", "lock"
  }) // RegexUtil; temp value used in pure method is equal up to equals but not up to ==
  @Pure
  @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
  public static boolean isRegex(char c) {
    return isRegex(Character.toString(c));
  }

  /**
   * Returns the argument as a {@code @Regex String} if it is a regex, otherwise throws an error.
   * The purpose of this method is to suppress Regex Checker warnings. It should be very rarely
   * needed.
   *
   * @param s string to check for being a regular expression
   * @return its argument
   * @throws Error if argument is not a regex
   */
  @SideEffectFree
  // The return type annotation is irrelevant; this method is special-cased by
  // RegexAnnotatedTypeFactory.
  public static @Regex String asRegex(String s) {
    return asRegex(s, 0);
  }

  /**
   * Returns the argument as a {@code @Regex(groups) String} if it is a regex with at least the
   * given number of groups, otherwise throws an error. The purpose of this method is to suppress
   * Regex Checker warnings. It should be very rarely needed.
   *
   * @param s string to check for being a regular expression
   * @param groups number of groups expected
   * @return its argument
   * @throws Error if argument is not a regex
   */
  @SuppressWarnings("regex") // RegexUtil
  @SideEffectFree
  // The return type annotation is irrelevant; this method is special-cased by
  // RegexAnnotatedTypeFactory.
  public static @Regex String asRegex(String s, int groups) {
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
   * Returns null if the argument is a syntactically valid regular expression. Otherwise returns a
   * string describing why the argument is not a regex.
   *
   * @param s string to check for being a regular expression
   * @return null, or a string describing why the argument is not a regex
   */
  @SideEffectFree
  public static @Nullable String regexError(String s) {
    return regexError(s, 0);
  }

  /**
   * Returns null if the argument is a syntactically valid regular expression with at least the
   * given number of groups. Otherwise returns a string describing why the argument is not a regex.
   *
   * @param s string to check for being a regular expression
   * @param groups number of groups expected
   * @return null, or a string describing why the argument is not a regex
   */
  @SuppressWarnings({"regex", "not.sef"}) // RegexUtil;
  @SideEffectFree
  public static @Nullable String regexError(String s, int groups) {
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
   * Returns null if the argument is a syntactically valid regular expression. Otherwise returns a
   * PatternSyntaxException describing why the argument is not a regex.
   *
   * @param s string to check for being a regular expression
   * @return null, or a PatternSyntaxException describing why the argument is not a regex
   */
  @SideEffectFree
  public static @Nullable PatternSyntaxException regexException(String s) {
    return regexException(s, 0);
  }

  /**
   * Returns null if the argument is a syntactically valid regular expression with at least the
   * given number of groups. Otherwise returns a PatternSyntaxException describing why the argument
   * is not a regex.
   *
   * @param s string to check for being a regular expression
   * @param groups number of groups expected
   * @return null, or a PatternSyntaxException describing why the argument is not a regex
   */
  @SuppressWarnings("regex") // RegexUtil
  @SideEffectFree
  public static @Nullable PatternSyntaxException regexException(String s, int groups) {
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
   * Generates an error message for s when expectedGroups are needed, but s only has actualGroups.
   *
   * @param s string to check for being a regular expression
   * @param expectedGroups the number of needed capturing groups
   * @param actualGroups the number of groups that {@code s} has
   * @return an error message for s when expectedGroups groups are needed, but s only has
   *     actualGroups groups
   */
  @SideEffectFree
  private static String regexErrorMessage(String s, int expectedGroups, int actualGroups) {
    return "regex \""
        + s
        + "\" has "
        + actualGroups
        + " groups, but "
        + expectedGroups
        + " groups are needed.";
  }

  /**
   * Returns the count of groups in the argument.
   *
   * @param p pattern whose groups to count
   * @return the count of groups in the argument
   */
  @SuppressWarnings("lock") // does not depend on object identity
  @Pure
  private static int getGroupCount(Pattern p) {
    return p.matcher("").groupCount();
  }

  /**
   * Return the strings such that any one of the regexes matches it.
   *
   * @param strings a collection of strings
   * @param regexes a collection of regular expressions
   * @return the strings such that any one of the regexes matches it
   */
  public static List<String> matchesSomeRegex(
      Collection<String> strings, Collection<@Regex String> regexes) {
    List<Pattern> patterns = mapList(Pattern::compile, regexes);
    List<String> result = new ArrayList<String>(strings.size());
    for (String s : strings) {
      for (Pattern p : patterns) {
        if (p.matcher(s).matches()) {
          result.add(s);
          break;
        }
      }
    }
    return result;
  }

  /**
   * Return true if every string is matched by at least one regex.
   *
   * @param strings a collection of strings
   * @param regexes a collection of regular expressions
   * @return true if every string is matched by at least one regex
   */
  public static boolean everyStringMatchesSomeRegex(
      Collection<String> strings, Collection<@Regex String> regexes) {
    List<Pattern> patterns = mapList(Pattern::compile, regexes);
    outer:
    for (String s : strings) {
      for (Pattern p : patterns) {
        if (p.matcher(s).matches()) {
          continue outer;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * Return the strings that are matched by no regex.
   *
   * @param strings a collection of strings
   * @param regexes a collection of regular expressions
   * @return the strings such that none of the regexes matches it
   */
  public static List<String> matchesNoRegex(
      Collection<String> strings, Collection<@Regex String> regexes) {
    List<Pattern> patterns = mapList(Pattern::compile, regexes);
    List<String> result = new ArrayList<String>(strings.size());
    outer:
    for (String s : strings) {
      for (Pattern p : patterns) {
        if (p.matcher(s).matches()) {
          continue outer;
        }
      }
      result.add(s);
    }
    return result;
  }

  /**
   * Return true if no string is matched by any regex.
   *
   * @param strings a collection of strings
   * @param regexes a collection of regular expressions
   * @return true if no string is matched by any regex
   */
  public static boolean noStringMatchesAnyRegex(
      Collection<String> strings, Collection<@Regex String> regexes) {
    for (String regex : regexes) {
      Pattern p = Pattern.compile(regex);
      for (String s : strings) {
        if (p.matcher(s).matches()) {
          return false;
        }
      }
    }
    return true;
  }

  //
  // Utilities
  //

  // This is from CollectionsPlume, but is here to make the file self-contained.

  /**
   * Applies the function to each element of the given iterable, producing a list of the results.
   *
   * <p>The point of this method is to make mapping operations more concise. Import it with
   *
   * <pre>import static org.plumelib.util.CollectionsPlume.mapList;</pre>
   *
   * This method is just like {@code transform}, but with the arguments in the other order.
   *
   * <p>To perform replacement in place, see {@code List.replaceAll}.
   *
   * @param <FROM> the type of elements of the given iterable
   * @param <TO> the type of elements of the result list
   * @param f a function
   * @param iterable an iterable
   * @return a list of the results of applying {@code f} to the elements of {@code iterable}
   */
  public static <
          @KeyForBottom FROM extends @Nullable @UnknownKeyFor Object,
          @KeyForBottom TO extends @Nullable @UnknownKeyFor Object>
      List<TO> mapList(
          @MustCallUnknown Function<@MustCallUnknown ? super FROM, ? extends TO> f,
          Iterable<FROM> iterable) {
    List<TO> result;

    if (iterable instanceof RandomAccess) {
      // Per the Javadoc of RandomAccess, an indexed for loop is faster than a foreach loop.
      List<FROM> list = (List<FROM>) iterable;
      int size = list.size();
      result = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        result.add(f.apply(list.get(i)));
      }
      return result;
    }

    if (iterable instanceof Collection) {
      result = new ArrayList<>(((Collection<?>) iterable).size());
    } else {
      result = new ArrayList<>(); // no information about size is available
    }
    for (FROM elt : iterable) {
      result.add(f.apply(elt));
    }
    return result;
  }
}
