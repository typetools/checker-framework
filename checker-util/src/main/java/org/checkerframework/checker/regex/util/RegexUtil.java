// This class should be kept in sync with org.plumelib.util.RegexUtil in the plume-util project.
// (Warning suppressions may differ.)

package org.checkerframework.checker.regex.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.nullness.qual.Nullable;
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
 * checker-qual.jar}, along with your binaries. Or, you can can copy this class into your own
 * project.
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
    @SuppressWarnings("regex") // RegexUtil; for purity, catches an exception
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
     * Returns true if the argument is a syntactically valid regular expression with at least {@code
     * groups} groups, and the specified groups are non-null if the regex matches.
     *
     * @param s string to check for being a regular expression
     * @param groups minimum number of groups
     * @param nonNullGroups groups that are non-null if the regex matches
     * @return true iff s is a regular expression with at least {@code groups} groups, and at least
     *     the groups in {@code nonNullGroups} are non-null when the regex matches
     */
    @Pure
    @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
    public static boolean isRegex(String s, int groups, int... nonNullGroups) {
        try {
            List<Integer> computedNonNullGroups = getNonNullGroups(s);
            if (groups <= getGroupCount(Pattern.compile(s))) {
                for (int e : nonNullGroups) {
                    if (!computedNonNullGroups.contains(e)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } catch (PatternSyntaxException | Error e) {
            return false;
        }
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
     *
     * <p>The purpose of this method is to suppress Regex Checker warnings. It should be very rarely
     * needed.
     *
     * @param s string to check for being a regular expression
     * @return its argument
     * @throws Error if argument is not a regex
     */
    @SideEffectFree
    // The return type annotation is a conservative bound, but is irrelevant because this method is
    // special-cased by RegexAnnotatedTypeFactory.
    public static @Regex String asRegex(String s) {
        return asRegex(s, 0);
    }

    /**
     * Returns the argument as a {@code @Regex(groups) String} if it is a regex with at least the
     * given number of groups, otherwise throws an error.
     *
     * <p>The purpose of this method is to suppress Regex Checker warnings. It should be very rarely
     * needed.
     *
     * @param s string to check for being a regular expression
     * @param groups number of groups expected
     * @return its argument
     * @throws Error if argument is not a regex with at least the given number of groups
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
     * Returns the argument as a {@code @Regex(groups) String} if it is a regex with at least the
     * given number of groups and the groups in {@code nonNullGroups} are guaranteed to match
     * provided that the regex matches a string. Otherwise, throws an error.
     *
     * <p>The purpose of this method is to suppress Regex Checker warnings. It should be rarely
     * needed.
     *
     * @param s string to check for being a regular expression
     * @param groups number of groups expected
     * @param nonNullGroups groups expected to be match some (possibly empty) part of a target
     *     string when the regex matches
     * @return its argument
     * @throws Error if argument is not a regex with the specified characteristics
     */
    @SuppressWarnings("regex")
    @SideEffectFree
    // The return type annotation is irrelevant; this method is special-cased by
    // RegexAnnotatedTypeFactory.
    public static @Regex String asRegex(String s, int groups, int... nonNullGroups) {
        try {
            int actualGroups = getGroupCount(Pattern.compile(s));
            if (groups > actualGroups) throw new Error(regexErrorMessage(s, groups, actualGroups));
            List<Integer> actualNonNullGroups = getNonNullGroups(s);
            int failingGroup = -1;
            for (int e : nonNullGroups) {
                if (!actualNonNullGroups.contains(e)) {
                    failingGroup = e;
                    break;
                }
            }
            if (failingGroup != -1) {
                throw new Error(regexNNGroupsErrorMessage(s, failingGroup));
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
     * Generates an error message for s when nullableGroup is expected to be definitely non-null but
     * turns out to be nullable.
     *
     * @param s string to check for being a regular expression
     * @param nullableGroup group expected to be non-null
     * @return an error message for s when nullableGroup is expected to be definitely non-null but
     *     turns out to be nullable.
     */
    private static String regexNNGroupsErrorMessage(String s, int nullableGroup) {
        return "for regex \""
                + s
                + "\", call to group("
                + nullableGroup
                + ") can return a possibly-null string "
                + " but is expected to return a non-null string.";
    }

    /**
     * Return the count of groups in the argument.
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
     * Returns a list of groups other than 0, that are guaranteed to be non-null given that the
     * regular expression matches a target String.
     *
     * @param regexp regular expression to be analysed; must be a legal regex
     * @return a {@code List} of groups that are guaranteed to match some part of a string that
     *     matches {@code regexp}
     * @throws Error if the argument is not a regex
     */
    public static List<Integer> getNonNullGroups(String regexp) {
        int groups = 0;
        try {
            Pattern p = Pattern.compile(regexp);
            groups = getGroupCount(p);
        } catch (PatternSyntaxException e) {
            throw new Error(e);
        }
        // The list that will hold the groups that are guaranteed to match some part of text that
        // matches the regex. Initially holds all the groups. The optional groups will be removed.
        List<Integer> nonNullGroups = new ArrayList<>();
        for (int i = 1; i <= groups; i++) {
            nonNullGroups.add(i);
        }

        // A stack containing indices of the capturing groups that are currently not closed.
        ArrayDeque<Integer> unclosedCapturingGroups = new ArrayDeque<>();

        // A stack that tracks all open groups (both capturing and non-capturing groups),
        // indicating whether the group is capturing or not.
        ArrayDeque<Boolean> isCapturingGroup = new ArrayDeque<>();

        // Index of the most recently opened capturing group.
        int group = 0;

        // Whenever we encounter any character, we know that it is not escaped. This is because,
        // when we encountered a '\' which was used to escape something, we traverse till the end
        // of the escape construct and then only resume normal traversal. This is why we are never
        // checking if the previous character was a '\'.

        // Optional group here onwards means the ith capturing group, which may not match any part
        // of a text that matched the regular expression and thus may return null on calls to
        // matcher.group(i).

        // Special construct means either pure, non-capturing groups that do not capture text and do
        // not count towards the group total.

        final int length = regexp.length();
        for (int i = 0; i < length; i++) {
            // Marks the opening of either a capturing a capturing group or some special
            // construct.
            if (regexp.charAt(i) == '(') {
                boolean isThisCapturingGroup = false;
                // If '(' is not followed by a '?' or is followed by '?<', it is a capturing group.
                if ((i < length - 1)
                        && (regexp.charAt(i + 1) != '?' || regexp.startsWith("?<", i + 1))) {
                    group += 1;
                    // Push the group index onto a stack.
                    unclosedCapturingGroups.push(group);
                    // This opening bracket marked the opening of a capturing group.
                    isThisCapturingGroup = true;
                }
                // Push the boolean on top of a stack. When we encounter a ')', we will use this
                // stack to check whether it closes a capturing group or a special construct.
                isCapturingGroup.push(isThisCapturingGroup);
                if (isThisCapturingGroup) {
                    // If this opening was preceded by a '|', this group is optional.
                    if (i > 0 && regexp.charAt(i - 1) == '|') {
                        nonNullGroups.remove(Integer.valueOf(group));
                    }
                }
            } else if (regexp.charAt(i) == ')') {
                // This closes the last opened "thing" (either a capturing group or a special
                // construct). What the last opened "thing" was, is determined from the boolean
                // stack.
                // If the top is false, then it was a special construct.
                // If the top is true, then it was a capturing group and we need to analyse it
                // further.
                boolean closesCapturingGroup = isCapturingGroup.pop();
                if (closesCapturingGroup) {
                    // The capturing group being closed is on top of the unclosedCapturingGroups
                    // stack.
                    Integer closedGroupIndex = unclosedCapturingGroups.pop();
                    // If the ')' is followed by a '?', '*', '|' or '{0', then this capturing group
                    // is optional and can be removed from the list of nonNullGroups.
                    if ((i < length - 1 && "?*|".contains(String.valueOf(regexp.charAt(i + 1))))
                            || (i < length - 2 && regexp.startsWith("{0", i + 1))) {
                        nonNullGroups.remove(closedGroupIndex);
                    }
                }
            } else if (regexp.charAt(i) == '[') {
                // A character class has been opened. We will traverse till we close this class.
                // Character classes can be nested, so we have a nestingLevel integer which keeps
                // track
                // of character classes still open.
                int nestingLevel = 1;
                // the loop starts from i+2 because the character class cannot be empty. "[]]" is a
                // valid regex.
                for (i = i + 1; i < length && nestingLevel > 0; i++) {
                    // A nested character class. Increment nestingLevel.
                    if (regexp.charAt(i) == '[') {
                        nestingLevel += 1;
                    }
                    // Either a nested character class being closed or a literal ']' (when
                    // immediately
                    // followed by an opening of a character class).
                    else if (regexp.charAt(i) == ']') {
                        // If the first character in a character class is "]", it is treated
                        // literally and does not close the character class.
                        if (regexp.charAt(i - 1) != '[') {
                            nestingLevel -= 1;
                        }
                    } else if (regexp.charAt(i) == '\\') {
                        // If a '\' is encountered, it may escape a single character or may
                        // represent a
                        // quote. Traverse till the end of the escape construct.
                        i = getLastIndexOfEscapeConstruct(regexp, i);
                    }
                }
                i = i - 1;
            } else if (regexp.charAt(i) == '\\') {
                // If a '\' is encountered, it may escape a single character or may represent a
                // quote. Traverse till the end of the escape construct.
                i = getLastIndexOfEscapeConstruct(regexp, i);
            }
        }
        return nonNullGroups;
    }

    /**
     * Returns the index till which the regex can be skipped, when '\' is encountered.
     *
     * @param regexp the regular expression to analyse
     * @param st the index of the '\' which causes the skip
     * @return the index till which the traversal can be skipped
     */
    private static int getLastIndexOfEscapeConstruct(String regexp, int st) {
        int length = regexp.length();
        if (st < length - 1 && regexp.charAt(st + 1) != 'Q') {
            return st + 1;
        }
        return regexp.indexOf("\\E", st) + 1;
    }
}
