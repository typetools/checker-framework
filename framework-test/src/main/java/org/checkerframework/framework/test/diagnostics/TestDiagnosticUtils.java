package org.checkerframework.framework.test.diagnostics;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.SystemUtil;

/** A set of utilities and factory methods useful for working with TestDiagnostics. */
public class TestDiagnosticUtils {

    /** How the diagnostics appear in Java source files. */
    public static final String DIAGNOSTIC_IN_JAVA_REGEX =
            "\\s*(error|fixable-error|warning|fixable-warning|other):\\s*(\\(?.*\\)?)\\s*";
    /** How the diagnostics appear in Java source files. */
    public static final Pattern DIAGNOSTIC_IN_JAVA_PATTERN =
            Pattern.compile(DIAGNOSTIC_IN_JAVA_REGEX);

    public static final String DIAGNOSTIC_WARNING_IN_JAVA_REGEX = "\\s*warning:\\s*(.*\\s*.*)\\s*";
    public static final Pattern DIAGNOSTIC_WARNING_IN_JAVA_PATTERN =
            Pattern.compile(DIAGNOSTIC_WARNING_IN_JAVA_REGEX);

    // How the diagnostics appear in javax tools diagnostics from the compiler.
    public static final String DIAGNOSTIC_REGEX = ":(\\d+):" + DIAGNOSTIC_IN_JAVA_REGEX;
    public static final Pattern DIAGNOSTIC_PATTERN = Pattern.compile(DIAGNOSTIC_REGEX);

    public static final String DIAGNOSTIC_WARNING_REGEX =
            ":(\\d+):" + DIAGNOSTIC_WARNING_IN_JAVA_REGEX;
    public static final Pattern DIAGNOSTIC_WARNING_PATTERN =
            Pattern.compile(DIAGNOSTIC_WARNING_REGEX);

    // How the diagnostics appear in diagnostic files (.out).
    public static final String DIAGNOSTIC_FILE_REGEX = ".+\\.java" + DIAGNOSTIC_REGEX;
    public static final Pattern DIAGNOSTIC_FILE_PATTERN = Pattern.compile(DIAGNOSTIC_FILE_REGEX);

    public static final String DIAGNOSTIC_FILE_WARNING_REGEX =
            ".+\\.java" + DIAGNOSTIC_WARNING_REGEX;
    public static final Pattern DIAGNOSTIC_FILE_WARNING_PATTERN =
            Pattern.compile(DIAGNOSTIC_FILE_WARNING_REGEX);

    /**
     * Instantiate the diagnostic based on a string that would appear in diagnostic files (i.e.
     * files that only contain line after line of expected diagnostics).
     *
     * @param stringFromDiagnosticFile a single diagnostic string to parse
     * @return a new TestDiagnostic
     */
    public static TestDiagnostic fromDiagnosticFileString(String stringFromDiagnosticFile) {
        return fromPatternMatching(
                DIAGNOSTIC_FILE_PATTERN,
                DIAGNOSTIC_WARNING_IN_JAVA_PATTERN,
                "",
                null,
                stringFromDiagnosticFile);
    }

    /**
     * Instantiate the diagnostic from a string that would appear in a Java file, e.g.: "error:
     * (message)"
     *
     * @param filename the file containing the diagnostic (and the error)
     * @param lineNumber the line number of the line immediately below the diagnostic comment in the
     *     Java file
     * @param stringFromjavaFile the string containing the diagnostic
     * @return a new TestDiagnostic
     */
    public static TestDiagnostic fromJavaFileComment(
            String filename, long lineNumber, String stringFromjavaFile) {
        return fromPatternMatching(
                DIAGNOSTIC_IN_JAVA_PATTERN,
                DIAGNOSTIC_WARNING_IN_JAVA_PATTERN,
                filename,
                lineNumber,
                stringFromjavaFile);
    }
    /**
     * Instantiate a diagnostic from output produced by the Java compiler. The resulting diagnostic
     * is never fixable and always has parentheses.
     */
    public static TestDiagnostic fromJavaxToolsDiagnostic(
            String diagnosticString, boolean noMsgText) {
        // It would be nice not to parse this from the diagnostic string.
        // However, the interface provides no way to know when an [unchecked] or similar
        // message is added to the reported error.  That is, when doing diagnostic.toString
        // the message may contain an [unchecked] even though getMessage does not report one.
        // Since we want to match the error messages reported by javac exactly, we must parse.
        Pair<String, String> trimmed = formatJavaxToolString(diagnosticString, noMsgText);
        return fromPatternMatching(
                DIAGNOSTIC_PATTERN,
                DIAGNOSTIC_WARNING_PATTERN,
                trimmed.second,
                null,
                trimmed.first);
    }

    /**
     * Instantiate the diagnostic from a JSpecify string that would appear in a Java file, e.g.:
     * "jspecify_some_category".
     *
     * @param filename the file containing the diagnostic (and the error)
     * @param lineNumber the line number of the line immediately below the diagnostic comment in the
     *     Java file
     * @param stringFromjavaFile the string containing the diagnostic
     * @return a new TestDiagnostic
     */
    public static TestDiagnostic fromJSpecifyFileComment(
            String filename, long lineNumber, String stringFromjavaFile) {
        return new TestDiagnostic(
                filename,
                lineNumber,
                DiagnosticKind.JSpecify,
                stringFromjavaFile,
                /*isFixable=*/ false,
                /*omitParentheses=*/ true);
    }

    /**
     * Instantiate the diagnostic via pattern-matching against patterns.
     *
     * @param diagnosticPattern a pattern that matches any diagnostic
     * @param warningPattern a pattern that matches a warning diagnostic
     * @param filename the file name
     * @param lineNumber the line number
     * @param diagnosticString the string to parse
     * @return a diagnostic parsed from the given string
     */
    @SuppressWarnings("nullness") // TODO: regular expression group access
    protected static TestDiagnostic fromPatternMatching(
            Pattern diagnosticPattern,
            Pattern warningPattern,
            String filename,
            @Nullable Long lineNumber,
            String diagnosticString) {
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
            Pair<DiagnosticKind, Boolean> categoryToFixable =
                    parseCategoryString(diagnosticMatcher.group(1 + groupOffset));
            kind = categoryToFixable.first;
            isFixable = categoryToFixable.second;
            String msg = diagnosticMatcher.group(2 + groupOffset).trim();
            noParentheses =
                    msg.equals("") || msg.charAt(0) != '(' || msg.charAt(msg.length() - 1) != ')';
            message = noParentheses ? msg : msg.substring(1, msg.length() - 1);

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

                // this should only happen if we are parsing a Java Diagnostic from the compiler
                // that we did do not handle
                if (lineNumber == null) {
                    lineNo = -1;
                }
            }
        }
        return new TestDiagnostic(filename, lineNo, kind, message, isFixable, noParentheses);
    }

    public static Pair<String, String> formatJavaxToolString(String original, boolean noMsgText) {
        String trimmed = original;
        String filename = "";
        if (noMsgText) {
            // Only keep the first line of the error or warning, unless it is a thrown exception
            // "unexpected Throwable" or it is a Checker Error (contains "Compilation unit").
            if (!trimmed.contains("unexpected Throwable")
                    && !trimmed.contains("Compilation unit")) {
                if (trimmed.contains(System.lineSeparator())) {
                    trimmed = trimmed.substring(0, trimmed.indexOf(System.lineSeparator()));
                }

                if (trimmed.contains(".java:")) {
                    int start = trimmed.lastIndexOf(File.separator);
                    filename = trimmed.substring(start + 1, trimmed.indexOf(".java:") + 5).trim();
                    trimmed = trimmed.substring(trimmed.indexOf(".java:") + 5).trim();
                }
            }
        }

        return Pair.of(trimmed, filename);
    }

    /**
     * Given a category string that may be prepended with "fixable-", return the category enum that
     * corresponds with the category and whether or not it is a isFixable error
     */
    private static Pair<DiagnosticKind, Boolean> parseCategoryString(String category) {
        final String fixable = "fixable-";
        final boolean isFixable = category.startsWith(fixable);
        if (isFixable) {
            category = category.substring(fixable.length());
        }
        DiagnosticKind categoryEnum = DiagnosticKind.fromParseString(category);
        if (categoryEnum == null) {
            throw new Error("Unparsable category: " + category);
        }

        return Pair.of(categoryEnum, isFixable);
    }

    /**
     * Return true if this line in a Java file indicates an expected diagnostic that might be
     * continued on the next line.
     */
    public static boolean isJavaDiagnosticLineStart(String originalLine) {
        final String trimmedLine = originalLine.trim();
        return trimmedLine.startsWith("// ::") || trimmedLine.startsWith("// warning:");
    }

    /**
     * Convert an end-of-line diagnostic message to a beginning-of-line one. Returns the argument
     * unchanged if it does not contain an end-of-line diagnostic message.
     *
     * <p>Most diagnostics in Java files start at the beginning of a line. Occasionally, javac
     * issues a warning about implicit code, such as an implicit constructor, on the line
     * <em>immediately after</em> a curly brace. The only place to put the expected diagnostic
     * message is on the line with the curly brace.
     *
     * <p>This implementation replaces "... { // ::" by "// ::", converting the end-of-line
     * diagnostic message to a beginning-of-line one that the rest of the code can handle. It is
     * rather specific (to avoid false positive matches, such as when "// ::" is commented out in
     * source code). It could be extended in the future if such an extension is necessary.
     */
    public static String handleEndOfLineJavaDiagnostic(String originalLine) {
        int curlyIndex = originalLine.indexOf("{ // ::");
        if (curlyIndex == -1) {
            return originalLine;
        } else {
            return originalLine.substring(curlyIndex + 2);
        }
    }

    /** Return true if this line in a Java file continues an expected diagnostic. */
    @EnsuresNonNullIf(result = true, expression = "#1")
    public static boolean isJavaDiagnosticLineContinuation(@Nullable String originalLine) {
        if (originalLine == null) {
            return false;
        }
        final String trimmedLine = originalLine.trim();
        // Unlike with errors, there is no logic elsewhere for splitting multiple "warning:"s.  So,
        // avoid concatenating them.  Also, each one must begin a line.  They are allowed to wrap to
        // the next line, though.
        return trimmedLine.startsWith("// ") && !trimmedLine.startsWith("// warning:");
    }

    /**
     * Return the continuation part. The argument is such that {@link
     * #isJavaDiagnosticLineContinuation} returns true.
     */
    public static String continuationPart(String originalLine) {
        return originalLine.trim().substring(2).trim();
    }

    /**
     * Convert a line in a Java source file to a TestDiagnosticLine.
     *
     * <p>The input {@code line} is possibly the concatenation of multiple source lines, if the
     * diagnostic was split across lines in the source code.
     */
    public static TestDiagnosticLine fromJavaSourceLine(
            String filename, String line, long lineNumber) {
        final String trimmedLine = line.trim();
        long errorLine = lineNumber + 1;

        if (trimmedLine.startsWith("// ::")) {
            String restOfLine = trimmedLine.substring(5); // drop the "// ::"
            String[] diagnosticStrs = restOfLine.split("::");
            List<TestDiagnostic> diagnostics =
                    SystemUtil.mapList(
                            (String diagnostic) ->
                                    fromJavaFileComment(filename, errorLine, diagnostic),
                            diagnosticStrs);
            return new TestDiagnosticLine(
                    filename, errorLine, line, Collections.unmodifiableList(diagnostics));

        } else if (trimmedLine.startsWith("// warning:")) {
            // This special diagnostic does not expect a line number nor a file name
            String diagnosticString = trimmedLine.substring(2);
            TestDiagnostic diagnostic = fromJavaFileComment("", 0, diagnosticString);
            return new TestDiagnosticLine("", 0, line, Collections.singletonList(diagnostic));
        } else if (trimmedLine.startsWith("//::")) {
            TestDiagnostic diagnostic =
                    new TestDiagnostic(
                            filename,
                            lineNumber,
                            DiagnosticKind.Error,
                            "Use \"// ::\", not \"//::\"",
                            false,
                            true);
            return new TestDiagnosticLine(
                    filename, lineNumber, line, Collections.singletonList(diagnostic));
        } else if (trimmedLine.startsWith("// jspecify_")) {
            TestDiagnostic diagnostic =
                    fromJSpecifyFileComment(filename, errorLine, trimmedLine.substring(3));
            return new TestDiagnosticLine(
                    filename, errorLine, line, Collections.singletonList(diagnostic));
        } else {
            // It's a bit gross to create empty diagnostics (returning null might be more
            // efficient), but they will be filtered out later.
            return new TestDiagnosticLine(filename, errorLine, line, Collections.emptyList());
        }
    }

    /** Convert a line in a DiagnosticFile to a TestDiagnosticLine. */
    public static TestDiagnosticLine fromDiagnosticFileLine(String diagnosticLine) {
        final String trimmedLine = diagnosticLine.trim();
        if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
            return new TestDiagnosticLine("", -1, diagnosticLine, Collections.emptyList());
        }

        TestDiagnostic diagnostic = fromDiagnosticFileString(diagnosticLine);
        return new TestDiagnosticLine(
                "", diagnostic.getLineNumber(), diagnosticLine, Arrays.asList(diagnostic));
    }

    public static Set<TestDiagnostic> fromJavaxDiagnosticList(
            List<Diagnostic<? extends JavaFileObject>> javaxDiagnostics, boolean noMsgText) {
        Set<TestDiagnostic> diagnostics = new LinkedHashSet<>(javaxDiagnostics.size());

        for (Diagnostic<? extends JavaFileObject> diagnostic : javaxDiagnostics) {
            // See TestDiagnosticUtils as to why we use diagnostic.toString rather
            // than convert from the diagnostic itself
            final String diagnosticString = diagnostic.toString();

            // suppress Xlint warnings
            if (diagnosticString.contains("uses unchecked or unsafe operations.")
                    || diagnosticString.contains("Recompile with -Xlint:unchecked for details.")
                    || diagnosticString.endsWith(" declares unsafe vararg methods.")
                    || diagnosticString.contains("Recompile with -Xlint:varargs for details.")) {
                continue;
            }

            diagnostics.add(
                    TestDiagnosticUtils.fromJavaxToolsDiagnostic(diagnosticString, noMsgText));
        }

        return diagnostics;
    }

    /**
     * Converts the given diagnostics to strings (as they would appear in a source file
     * individually).
     *
     * @param diagnostics a list of diagnostics
     * @return a list of the diagnastics as they would appear in a source file
     */
    public static List<String> diagnosticsToString(List<TestDiagnostic> diagnostics) {
        return SystemUtil.mapList(TestDiagnostic::toString, diagnostics);
    }

    public static void removeDiagnosticsOfKind(
            DiagnosticKind kind, List<TestDiagnostic> expectedDiagnostics) {
        for (int i = 0; i < expectedDiagnostics.size(); /*no-increment*/ ) {
            if (expectedDiagnostics.get(i).getKind() == kind) {
                expectedDiagnostics.remove(i);
            } else {
                ++i;
            }
        }
    }
}
