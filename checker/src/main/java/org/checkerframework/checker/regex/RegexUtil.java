// This class should be kept in sync with plume.RegexUtil.

package org.checkerframework.checker.regex;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

/**
 * Utility methods for regular expressions, most notably for testing whether a string is a regular
 * expression.
 *
 * <p>For an example of intended use, see section <a
 * href="https://checkerframework.org/manual/#regexutil-methods">Testing whether a string is a
 * regular expression</a> in the Checker Framework manual.
 *
 * <p><b>Runtime Dependency</b>: Using this class introduces a runtime dependency. This means that
 * you need to distribute (or link to) the Checker Framework, along with your binaries. To eliminate
 * this dependency, you can simply copy this class into your own project.
 */
// The Purity Checker cannot show for most methods in this class that
// they are pure, even though they are.
@SuppressWarnings("purity")
public final class RegexUtil {

    /** This class is a collection of methods; it does not represent anything. */
    private RegexUtil() {
        throw new Error("do not instantiate");
    }

    /**
     * A checked version of {@link PatternSyntaxException}.
     *
     * <p>This exception is useful when an illegal regex is detected but the contextual information
     * to report a helpful error message is not available at the current depth in the call stack. By
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
         * @param index the approximate index in the pattern of the error, or {@code -1} if the
         *     index is not known
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
         * @return the approximate index in the pattern of the error, or {@code -1} if the index is
         *     not known
         */
        public int getIndex() {
            return pse.getIndex();
        }

        /**
         * Returns a multi-line string containing the description of the syntax error and its index,
         * the erroneous regular-expression pattern, and a visual indication of the error index
         * within the pattern.
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
    @SuppressWarnings({"regex", "deterministic"}) // RegexUtil; for purity, catches an exception
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
        "regex",
        "purity.not.deterministic.call",
        "lock"
    }) // RegexUtil; temp value used in pure method is equal up to equals but not up to ==
    @Pure
    @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
    public static boolean isRegex(final char c) {
        return isRegex(Character.toString(c));
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
     * given number of groups. Otherwise returns a string describing why the argument is not a
     * regex.
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
     * given number of groups. Otherwise returns a PatternSyntaxException describing why the
     * argument is not a regex.
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
                return new PatternSyntaxException(
                        regexErrorMessage(s, groups, actualGroups), s, -1);
            }
        } catch (PatternSyntaxException pse) {
            return pse;
        }
        return null;
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
    // The return type annotation is a conservative bound.
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
    // The return type annotation is irrelevant; it is special-cased by
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
     * Return the count of groups in the argument.
     *
     * @param p pattern whose groups to count
     * @return the count of groups in the argument
     */
    @SuppressWarnings({"purity", "lock"}) // does not depend on object identity
    @Pure
    private static int getGroupCount(Pattern p) {
        return p.matcher("").groupCount();
    }
}
