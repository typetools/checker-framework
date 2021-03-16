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
        Pattern p;
        try {
            p = Pattern.compile(s);
        } catch (PatternSyntaxException e) {
            return false;
        }
        List<Integer> computedNonNullGroups = getNonNullGroups(p.pattern(), getGroupCount(p));
        return groups <= getGroupCount(p)
                && computedNonNullGroups.containsAll(Arrays.asList(nonNullGroups));
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
    // The return type annotation is irrelevant; it is special-cased by
    // RegexAnnotatedTypeFactory.
    // The return type annotation is a conservative bound.
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
            List<Integer> actualNonNullGroups = getNonNullGroups(s, groups);
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
     * regular expression matches the String. The String argument passed has to be a valid regular
     * expression.
     *
     * @param regexp regular expression to be analysed; must be a legal regex
     * @param n number of capturing groups in the pattern
     * @return a {@code List} of groups that are guaranteed to match some part of a string that
     *     matches {@code regexp}
     * @throws Error if the argument is not a regex or has less than the specified number of
     *     capturing groups
     */
    public static List<Integer> getNonNullGroups(String regexp, int n) {
        try {
            Pattern p = Pattern.compile(regexp);
            int actualGroups = getGroupCount(p);
            if (actualGroups < n) {
                throw new Error(regexErrorMessage(regexp, n, actualGroups));
            }
        } catch (PatternSyntaxException e) {
            throw new Error(e);
        }
        // The list that will hold the groups that are guaranteed to match some part of text that
        // matches the regex. Initially holds all the groups. The optional groups will be removed.
        List<Integer> nonNullGroups = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            nonNullGroups.add(i);
        }

        // A stack containing indices of the capturing groups that are currently not closed.
        ArrayDeque<Integer> unclosedCapturingGroups = new ArrayDeque<>();

        // A stack that tracks all open groups (both capturing and non-capturing groups),
        // indicating whether the group is capturing or not.
        ArrayDeque<Boolean> isCapturingGroup = new ArrayDeque<>();

        // Index of the most recently opened capturing group.
        int group = 0;

        // Optional group here onwards means the ith capturing group, which may not match any part
        // of a text that matched the regular expression and thus may return null on calls to
        // matcher.group(i).

        // If you encounter '(', check the next character. If it is a '?', it is a special
        // construct,
        // (either pure, non-capturing groups that do not capture text and do not count towards the
        // group total, or named-capturing group) and we need to check the next character. If the
        // character following the '?' is a '<' the '(' represents the opening of a named-capturing
        // group and it will be handled like a normal capturing group. If the '(' was not followed
        // by a '?', it is a normal capturing group.
        // In case of capturing groups, increment the group variable and push it to the
        // unclosedCapturingGroups deque. Push true to the isCapturingGroup deque.
        // In case of non-capturing groups, push false to the isCapturingGroup deque.
        // We need the boolean deque so that when we encounter a ')', we can know whether it closes
        // a capturing group (the top element is true) or a non-capturing group (the top element is
        // false). One additional check is required. If '(' represented a capturing a group and was
        // preceded by '|', it is an optional group.

        // If you encounter ')', check the top of the isCapturingGroup deque. If it was
        // false, do nothing otherwise remove the top element from the deque since it is now closed
        // and check if it is followed by a '?', '*', '|' or '{0'. If it is, then it is an optional
        // group otherwise not. If it is an optional group, remove it from the nonNullGroups list.

        // If you encounter '[', traverse the regex till you find the closing '[', you may encounter
        // more of '[' in the process, keep a track of the number of character classes that are
        // still open. Keep on traversing till the number becomes 0. After this resume normal
        // traversal.

        // If you encounter '\', check the next character. If it is not 'Q', skip the next
        // character. If it is 'Q', it marks the beginning of a literal quote, find the next
        // occurrence of '\E', which marks the end of quote. Set the loop variable to the index of
        // 'E' and resume normal traversal.

        final int length = regexp.length();
        for (int i = 0; i < length; i++) {
            if (regexp.charAt(i) == '(') {
                boolean isCapturingGroup = false;
                if ((i < length - 1)
                        && (regexp.charAt(i + 1) != '?' || regexp.startsWith("?<", i + 1))) {
                    group += 1;
                    unclosedCapturingGroups.push(group);
                    isCapturingGroup = true;
                }
                isCapturingGroup.push(isCapturingGroup);
                if (isCapturingGroup) {
                    if (i > 0 && regexp.charAt(i - 1) == '|') {
                        nonNullGroups.remove(Integer.valueOf(group));
                    }
                }
            } else if (regexp.charAt(i) == ')') {
                boolean closesCapturingGroup = isCapturingGroup.pop();
                if (closesCapturingGroup) {
                    Integer closedGroupIndex = unclosedCapturingGroups.pop();
                    if ((i < length - 1 && "?*|".contains(String.valueOf(regexp.charAt(i + 1))))
                            || (i < length - 2 && regexp.startsWith("{0", i + 1))) {
                        nonNullGroups.remove(closedGroupIndex);
                    }
                }
            } else if (regexp.charAt(i) == '[') {
                int balance = 1, j;
                // the loop starts from i+2 because the character class cannot be empty. "[]]" is a
                // valid regex.
                for (j = i + 1; j < length && balance > 0; j++) {
                    if (regexp.charAt(j) == '[') {
                        // Character classes can be nested.
                        balance += 1;
                    } else if (regexp.charAt(j) == ']') {
                        // If the first character in a character class is "]", it is treated
                        // literally and does not close the character class.
                        if (regexp.charAt(j - 1) != '[') {
                            balance -= 1;
                        }
                    } else if (regexp.charAt(j) == '\\') {
                        j = resumeTraversalFromHere(regexp, j);
                    }
                }
                i = j - 1;
            } else if (regexp.charAt(i) == '\\') {
                i = resumeTraversalFromHere(regexp, i);
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
    private static int resumeTraversalFromHere(String regexp, int st) {
        int length = regexp.length();
        if (st < length - 1 && regexp.charAt(st + 1) != 'Q') {
            return st + 1;
        }
        return regexp.indexOf("\\E", st) + 1;
    }
}
