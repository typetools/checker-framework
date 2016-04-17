package org.checkerframework.framework.test.diagnostics;

import org.checkerframework.javacutil.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * A set of utilities and factory methods useful for working with TestDiagnostics
 */
public class TestDiagnosticUtils {

    public static final String STUB_PARSER_STRING = "warning: StubParser";
    public static final String STUB_PARSER_COMMENT = "//" + STUB_PARSER_STRING;

    //This is SPARTA specific and should be removed, we need to create a more general way to handle these special
    //diagnostics, perhaps by moving away from static state
    public static final String FLOW_POLICY_STRING = "warning: FlowPolicy:";
    public static final String FLOW_POLICY_COMMENT = "//" + FLOW_POLICY_STRING;

    //this regex represents how the diagnostics appear in Java source files
    public static final String DIAGNOSTIC_IN_JAVA_REGEX = "\\s*(error|fixable-error|warning|other):\\s*(\\(?.*\\)?)\\s*";
    public static final Pattern DIAGNOSTIC_IN_JAVA_PATTERN = Pattern.compile(DIAGNOSTIC_IN_JAVA_REGEX);

    public static final String DIAGNOSTIC_WARNING_IN_JAVA_REGEX = "\\s*warning:\\s*(.*\\s*.*)\\s*";
    public static final Pattern DIAGNOSTIC_WARNING_IN_JAVA_PATTERN = Pattern.compile(DIAGNOSTIC_WARNING_IN_JAVA_REGEX);

    //this regex represents how the diagnostics appear in javax tools diagnostics from the compiler
    public static final String DIAGNOSTIC_REGEX = ":(\\d+):" + DIAGNOSTIC_IN_JAVA_REGEX;
    public static final Pattern DIAGNOSTIC_PATTERN = Pattern.compile(DIAGNOSTIC_REGEX);

    public static final String DIAGNOSTIC_WARNING_REGEX = ":(\\d+):" + DIAGNOSTIC_WARNING_IN_JAVA_REGEX;
    public static final Pattern DIAGNOSTIC_WARNING_PATTERN = Pattern.compile(DIAGNOSTIC_WARNING_REGEX);

    //represents how the diagnostics appearn in diagnostic files (.out)
    public static final String DIAGNOSTIC_FILE_REGEX = ".+\\.java" + DIAGNOSTIC_REGEX;
    public static final Pattern DIAGNOSTIC_FILE_PATTERN = Pattern.compile(DIAGNOSTIC_FILE_REGEX);

    public static final String DIAGNOSTIC_FILE_WARNING_REGEX = ".+\\.java" + DIAGNOSTIC_WARNING_REGEX;
    public static final Pattern DIAGNOSTIC_FILE_WARNING_PATTERN = Pattern.compile(DIAGNOSTIC_FILE_WARNING_REGEX);

    /**
     * Instantiate the diagnostic based on a string that would appear in diagnostic files
     * (i.e. files that only contain line after line of expected diagnostics)
     * @param stringFromDiagnosticFile A single diagnostic string to parse
     */
    public static TestDiagnostic fromDiagnosticFileString(String stringFromDiagnosticFile) {
        return fromPatternMatching(DIAGNOSTIC_FILE_PATTERN, DIAGNOSTIC_WARNING_IN_JAVA_PATTERN,
                                  null, stringFromDiagnosticFile);
    }

    /**
     * Instantiate the diagnostic from a string that would appear in a Java file, e.g.:
     * "error: (message)"
     * @param lineNumber The lineNumber of the line immediately below the diagnostic comment in the Java file
     * @param stringFromjavaFile The string containing the diagnostic
     */
    public static TestDiagnostic fromJavaFileComment(long lineNumber, String stringFromjavaFile) {
        return fromPatternMatching(DIAGNOSTIC_IN_JAVA_PATTERN, DIAGNOSTIC_WARNING_IN_JAVA_PATTERN,
                lineNumber, stringFromjavaFile);
    }
    /**
     * Instantiate a diagnostic using a diagnostic from the Java Compiler.  The resulting diagnostic
     * is never fixable and always has parentheses
     */
    public static TestDiagnostic fromJavaxToolsDiagnostic(String diagnosticString, boolean noMsgText) {
        //It would be nice not to parse this from the diagnostic string
        //however, the interface provides no way to know when an [unchecked] or similar
        //message is added to the reported error.  That is, when doing diagnostic.toString
        //the message may contain an [unchecked] even though getMessage does not report one
        //Since we want to match the error messages reported by javac exactly, we must parse
        String trimmed = formatJavaxToolString(diagnosticString, noMsgText);
        return fromPatternMatching(DIAGNOSTIC_PATTERN, DIAGNOSTIC_WARNING_PATTERN, null, trimmed);
    }


    static Pair<Boolean, String> dropParentheses(final String str) {
        if (str.charAt(0) == '(' && str.charAt(str.length()-1) == ')') {
            return Pair.of(true, str.substring(1, str.length()-1));
        }
        return Pair.of(false, str);
    }

    protected static TestDiagnostic fromPatternMatching(Pattern diagnosticPattern, Pattern warningPattern,
                                                        Long lineNumber, String diagnosticString) {
        final DiagnosticKind kind;
        final String message;
        final boolean isFixable;
        final boolean noParentheses;
        long lineNo = -1;
        int groupOffset = 1;

        if (lineNumber != null) {
            lineNo = lineNumber;
            groupOffset = 0;
        }

        Matcher diagnosticMatcher = diagnosticPattern.matcher(diagnosticString);
        if (diagnosticMatcher.matches()) {
            Pair<DiagnosticKind, Boolean> categoryToFixable = parseCategoryString(diagnosticMatcher.group(1 + groupOffset));
            kind = categoryToFixable.first;
            isFixable = categoryToFixable.second;
            Pair<Boolean, String> dropQuotesToString = dropParentheses(diagnosticMatcher.group(2 + groupOffset).trim());
            message = dropQuotesToString.second;
            noParentheses = !dropQuotesToString.first;

            if (lineNumber == null) {
                lineNo = Long.parseLong(diagnosticMatcher.group(1));
            }

        } else {
            Matcher warningMatcher = warningPattern.matcher(diagnosticString);
            if (warningMatcher.matches()) {
                kind = DiagnosticKind.Warning;
                isFixable = false;
                message = warningMatcher.group(1 + groupOffset);
                noParentheses = true;

                if (lineNumber == null) {
                    lineNo = Long.parseLong(diagnosticMatcher.group(1));
                }

            } else if (diagnosticString.startsWith("warning:")) {
                kind = DiagnosticKind.Warning;
                isFixable = false;
                message = diagnosticString.substring("warning:".length()).trim();
                noParentheses = true;
                if (lineNumber != null) {
                    lineNo = lineNumber;
                } else {
                    lineNo = 0;
                }

            } else {
                kind = DiagnosticKind.Other;
                isFixable = false;
                message = diagnosticString;
                noParentheses = true;

                //this should only happen if we are parsing a Java Diagnostic from the compiler
                //that we did do not handle
                if (lineNumber == null) {
                    lineNo = -1;
                }
            }
        }
        return new TestDiagnostic(lineNo, kind, message, isFixable, noParentheses);
    }

    public static String formatJavaxToolString(String original, boolean noMsgText) {
        String trimmed = original;

        if (noMsgText) {
            if (!trimmed.contains("unexpected Throwable")) {
                if (trimmed.contains("\n")) {
                    trimmed = trimmed.substring(0, trimmed.indexOf('\n'));
                }

                if (trimmed.contains(".java:")) {
                    trimmed = trimmed.substring(trimmed.indexOf(".java:") + 5).trim();
                }
            }
        }

        return trimmed;
    }

    /**
     * Given a category string that may be prepended with "fixable-", return the category
     * enum that corresponds with the category and whether or not it is a isFixable error
     */
    private static Pair<DiagnosticKind, Boolean> parseCategoryString(String category) {
        final String fixable = "fixable-";
        final boolean isFixable = category.startsWith(fixable);
        if (isFixable) {
            category = category.substring(fixable.length());
        }
        DiagnosticKind categoryEnum = DiagnosticKind.fromParseString(category);

        return Pair.of(categoryEnum, isFixable);
    }

    /**
     * Convert a line in a JavaSource file to a (possibly empty) TestDiagnosticLine
     */
    public static TestDiagnosticLine fromJavaSourceLine(String originalLine, long lineNumber) {
        final String trimmedLine = originalLine.trim();
        long errorLine = lineNumber + 1;

        //TODO: see comments on FLOW_POLICY_COMMENT
        final boolean normalDiagnostic = trimmedLine.startsWith("//::");
        if (normalDiagnostic || trimmedLine.startsWith("//warning:")) {

            String[] diagnosticStrs;
            if (normalDiagnostic) {
                diagnosticStrs =
                    trimmedLine
                        .substring(4) // drop the //::
                        .split("::");
            } else {
                diagnosticStrs = new String[]{ trimmedLine.substring(2) };
            }

            List<TestDiagnostic> diagnostics = new ArrayList<>(diagnosticStrs.length);
            for (String diagnostic : diagnosticStrs) {
                diagnostics.add(fromJavaFileComment((normalDiagnostic) ? errorLine : 0, diagnostic));
            }

            return new TestDiagnosticLine(errorLine, originalLine, Collections.unmodifiableList(diagnostics));

        } else {
            return new TestDiagnosticLine(errorLine, originalLine, EMPTY);

        }
    }

    /**
     * Convert a line in a DiagnosticFile to a TestDiagnosticLine
     */
    public static TestDiagnosticLine fromDiagnosticFileLine(String diagnosticLine) {
        final String trimmedLine = diagnosticLine.trim();
        if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
            return new TestDiagnosticLine(-1, diagnosticLine, EMPTY);
        }

        TestDiagnostic diagnostic = fromDiagnosticFileString(diagnosticLine);
        return new TestDiagnosticLine(diagnostic.getLineNumber(), diagnosticLine, Arrays.asList(diagnostic));
    }

    public static Set<TestDiagnostic> fromJavaxDiagnosticList(List<Diagnostic<? extends JavaFileObject>> javaxDiagnostics,
                                                              boolean noMsgText) {
        Set<TestDiagnostic> diagnostics = new LinkedHashSet<>(javaxDiagnostics.size());

        for (Diagnostic<? extends JavaFileObject> diagnostic : javaxDiagnostics) {
            //See TestDiagnosticUtils as to why we use diagnostic.toString rather
            //than convert from the diagnostic itself
            final String diagnosticString = diagnostic.toString();

            // suppress Xlint warnings
            if (diagnosticString.contains("uses unchecked or unsafe operations.") ||
                diagnosticString.contains("Recompile with -Xlint:unchecked for details.") ||
                diagnosticString.endsWith(" declares unsafe vararg methods.") ||
                diagnosticString.contains("Recompile with -Xlint:varargs for details."))
                continue;

            diagnostics.add(TestDiagnosticUtils.fromJavaxToolsDiagnostic(diagnosticString, noMsgText));
        }

        return diagnostics;
    }

    /**
     * Converts the given diagnostics to strings (as they would appear in a source file individually)
     */
    public static List<String> diagnosticsToString(List<TestDiagnostic> diagnostics) {
        final List<String> strings = new ArrayList<String>(diagnostics.size());
        for (TestDiagnostic diagnostic : diagnostics) {
            strings.add(diagnostic.toString());
        }
        return strings;
    }

    private static final List<TestDiagnostic> EMPTY = Collections.unmodifiableList(new ArrayList<TestDiagnostic>());

    public static void removeDiagnosticsOfKind(DiagnosticKind kind, List<TestDiagnostic> expectedDiagnostics) {
        for (int i = 0; i < expectedDiagnostics.size(); /*no-increment*/) {
            if (expectedDiagnostics.get(i).getKind() == kind) {
                expectedDiagnostics.remove(i);
            } else {
                ++i;
            }
        }
    }
}
