package org.checkerframework.eclipse.javac;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.error.CheckerErrorStatus;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.checkerframework.eclipse.util.Util;
import org.eclipse.ui.statushandlers.StatusManager;

/** Error reported by javac. Created by parsing javac output. */
public class JavacError {
    private static final boolean VERBOSE = true;
    public final File file;
    public final int lineNumber;
    public final String errorKey;
    public final List<String> errorArguments;
    public final String message;
    public final Diagnostic<? extends JavaFileObject> diag;
    public final int startPosition;
    public final int endPosition;

    public JavacError(
            File file,
            int lineNumber,
            String errorKey,
            List<String> errorArguments,
            String message,
            int startPosition,
            int endPosition,
            Diagnostic<? extends JavaFileObject> diag) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.message = message;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.errorKey = errorKey;
        this.errorArguments = errorArguments;
        this.diag = diag;
    }

    public JavacError(Diagnostic<? extends JavaFileObject> diag) {
        this(
                new File(diag.getSource().toUri().getPath()),
                (int) diag.getLineNumber(),
                null,
                null,
                diag.getMessage(null),
                (int) diag.getStartPosition(),
                (int) diag.getEndPosition(),
                diag);
    }

    public JavacError(File file, int lineNumber, String message) {
        this(file, lineNumber, null, null, message, -1, -1, null);
    }

    public JavacError(
            File file,
            int lineNumber,
            String errorKey,
            List<String> errorArguments,
            String message,
            int startPosition,
            int endPosition) {
        this(file, lineNumber, errorKey, errorArguments, message, startPosition, endPosition, null);
    }

    @Override
    public String toString() {
        return file.getPath() + ":" + lineNumber + ": " + message;
    }

    /** Parses javac output and converts to a list of errors. */
    private static final Pattern errorCountPattern = Pattern.compile("^[0-9]+ (error|warning)*s?$");

    private static final Pattern noncheckerPattern = Pattern.compile("^(Note|warning|error): .* $");
    private static final String headMessagePatternString =
            "^(.*):(\\d*): (?:(?:warning|error)?: ?)?\\((.*)\\) \\$\\$ (\\d*) ";
    private static final Pattern headMessagePattern =
            Pattern.compile(headMessagePatternString + ".*");

    private static final Pattern trimmingPattern =
            Pattern.compile(headMessagePatternString + "(.*)");

    private static final String argumentMessagePatternString = "\\$\\$ (.*) ";
    private static final String tailMessagePatternString =
            "\\$\\$ (?:(?:\\( (-?\\d+), (-?\\d+) \\))|null) \\$\\$ (.*)$";
    private static final Pattern noProcessorPattern =
            Pattern.compile("^error: Annotation processor (.*) not found$");
    private static final Pattern invalidFlagPattern =
            Pattern.compile("^javac: invalid flag: (.*)$");
    private static final Pattern missingFilePattern =
            Pattern.compile("^error: Could not find class file for (.*)\\.$");

    private static Pattern createCompletePattern(int numberOfArguments) {
        StringBuilder sb = new StringBuilder();
        sb.append(headMessagePatternString);
        for (int i = 0; i < numberOfArguments; ++i) {
            sb.append(argumentMessagePatternString);
        }
        sb.append(tailMessagePatternString);
        return Pattern.compile(sb.toString());
    }

    /**
     * If errorStr matches the expected pattern of an error report this method will return the part
     * of the errorStr WITHOUT the adetailedmsg information. Otherwise errorStr is just returned.
     */
    public static String trimDetails(final String errorStr) {
        final Matcher matcher = trimmingPattern.matcher(errorStr);
        if (matcher.matches()) {
            int shave = matcher.group(4).length() + matcher.group(5).length() + " $$ ".length();
            return errorStr.substring(0, errorStr.length() - shave);
        }

        return errorStr;
    }

    public static List<JavacError> parse(String javacoutput) {
        if (VERBOSE) System.out.println("javac output:\n" + javacoutput);
        if (javacoutput == null) return null;

        List<JavacError> result = new ArrayList<JavacError>();
        List<String> lines = Arrays.asList(javacoutput.split(Util.NL));

        if (!handleErrors(lines.get(0))) {
            return result;
        }

        File errorFile = null;
        int lineNum = 0;
        StringBuilder messageBuilder = new StringBuilder();
        Iterator<String> iter = lines.iterator();
        String errorKey = null;
        int startPosition = -1;
        int endPosition = -1;
        int numberOfArguments = -1;
        List<String> errorArguments = null;

        while (iter.hasNext()) {
            String line = iter.next();
            Matcher matcher = headMessagePattern.matcher(line.trim());
            if (matcher.matches()) {
                if (errorFile != null) {
                    JavacError error =
                            new JavacError(
                                    errorFile,
                                    lineNum,
                                    errorKey,
                                    errorArguments,
                                    messageBuilder.toString().trim(),
                                    startPosition,
                                    endPosition);
                    result.add(error);
                }
                errorFile = new File(matcher.group(1));
                lineNum = Integer.parseInt(matcher.group(2));
                errorKey = matcher.group(3);
                numberOfArguments = Integer.parseInt(matcher.group(4));
                Matcher completeMatcher =
                        createCompletePattern(numberOfArguments).matcher(line.trim());
                errorArguments = new ArrayList<String>();
                messageBuilder = new StringBuilder();
                if (completeMatcher.matches()) {
                    for (int i = 0; i < numberOfArguments; ++i) {
                        errorArguments.add(completeMatcher.group(5 + i));
                    }
                    startPosition = Integer.parseInt(completeMatcher.group(5 + numberOfArguments));
                    endPosition = Integer.parseInt(completeMatcher.group(6 + numberOfArguments));
                    if (endPosition < startPosition) {
                        endPosition = startPosition;
                    }
                    messageBuilder.append(completeMatcher.group(7 + numberOfArguments));
                }
                messageBuilder.append(Util.NL);
            } else {
                if (errorCountPattern.matcher(line).matches() || !iter.hasNext()) {
                    if (messageBuilder.length() != 0) {
                        JavacError error =
                                new JavacError(
                                        errorFile,
                                        lineNum,
                                        errorKey,
                                        errorArguments,
                                        messageBuilder.toString().trim(),
                                        startPosition,
                                        endPosition);
                        result.add(error);
                    }
                } else if (!line.trim().equals("^") && !noncheckerPattern.matcher(line).matches()) {
                    messageBuilder.append(line);
                    messageBuilder.append(Util.NL);
                }
            }
        }

        // filter out for errors/warnings matching a regex
        String filterRegex =
                CheckerPlugin.getDefault()
                        .getPreferenceStore()
                        .getString(CheckerPreferences.PREF_CHECKER_ERROR_FILTER_REGEX);
        if (!filterRegex.isEmpty()) {
            Iterator<JavacError> errorIter = result.iterator();
            while (errorIter.hasNext()) {
                JavacError err = errorIter.next();
                Matcher filterMatcher =
                        Pattern.compile(filterRegex, Pattern.DOTALL).matcher(err.message);
                if (filterMatcher.matches()) {
                    errorIter.remove();
                }
            }
        }

        return result;
    }

    /**
     * Handle special error output cases for the compiler.
     *
     * @return true if no errors are present, false otherwise
     */
    private static boolean handleErrors(String line) {
        StatusManager manager = StatusManager.getManager();

        // special case for missing checkers.jar (or processor class)
        Matcher procMatcher = noProcessorPattern.matcher(line);
        if (procMatcher.matches()) {
            CheckerErrorStatus status;

            if (procMatcher.group(1).equals("''")) {
                status =
                        new CheckerErrorStatus(
                                "No checkers configured. Use the plugin preferences to configure checkers to use.");
            } else {
                status =
                        new CheckerErrorStatus(
                                "Annotation processor "
                                        + procMatcher.group(1)
                                        + " could not be found. Try adding checkers.jar to your project build path.");
            }
            manager.handle(status, StatusManager.SHOW);
            return false;
        }

        // Misc errors that prevent compiler from running:
        Matcher flagMatcher = invalidFlagPattern.matcher(line);
        if (flagMatcher.matches()) {
            manager.handle(
                    new CheckerErrorStatus(
                            "Invalid compiler flag: "
                                    + flagMatcher.group(1)
                                    + ". Check your preferences for invalid flags."),
                    StatusManager.SHOW);
            return false;
        }

        Matcher missingFileMatcher = missingFilePattern.matcher(line);
        if (missingFileMatcher.matches()) {
            manager.handle(
                    new CheckerErrorStatus(
                            "Cannot find file: "
                                    + missingFileMatcher.group(1)
                                    + ". You may have malformed input in your preferences."),
                    StatusManager.SHOW);
            return false;
        }

        return true;
    }
}
