package checkers.eclipse.javac;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.eclipse.ui.statushandlers.StatusManager;

import checkers.eclipse.CheckerPlugin;
import checkers.eclipse.error.CheckerErrorStatus;
import checkers.eclipse.prefs.CheckerPreferences;
import checkers.eclipse.util.Util;

/**
 * Error reported by javac. Created by parsing javac output.
 */
public class JavacError
{
    private static final boolean VERBOSE = true;
    public final File file;
    public final int lineNumber;
    public final String message;
    public final Diagnostic<? extends JavaFileObject> diag;

    public JavacError(File file, int lineNumber, String message)
    {
        this.file = file;
        this.lineNumber = lineNumber;
        this.message = message;
        this.diag = null;
    }

    @Override
    public String toString()
    {
        return file.getPath() + ":" + lineNumber + ": " + message;
    }

    /**
     * Parses javac output and converts to a list of errors.
     */
    private static final Pattern errorCountPattern = Pattern
            .compile("^[0-9]+ (error|warning)*s?$");
    private static final Pattern noncheckerPattern = Pattern
            .compile("^(Note|warning|error): .* $");
    private static final Pattern messagePattern = Pattern
            .compile("^(.*):(\\d*): (?:(?:warning|error)?: ?)?(.*)$");
    private static final Pattern noProcessorPattern = Pattern
            .compile("^error: Annotation processor (.*) not found$");
    private static final Pattern invalidFlagPattern = Pattern
            .compile("^javac: invalid flag: (.*)$");
    private static final Pattern missingFilePattern = Pattern
            .compile("^error: Could not find class file for (.*)\\.$");

    public static List<JavacError> parse(String javacoutput)
    {
        if (VERBOSE)
            System.out.println("javac output:\n" + javacoutput);
        if (javacoutput == null)
            return null;

        List<JavacError> result = new ArrayList<JavacError>();
        List<String> lines = Arrays.asList(javacoutput.split(Util.NL));

        if (!handleErrors(lines.get(0)))
        {
            return result;
        }

        File errorFile = null;
        int lineNum = 0;
        StringBuilder messageBuilder = new StringBuilder();
        Iterator<String> iter = lines.iterator();

        while (iter.hasNext())
        {
            String line = iter.next();
            Matcher matcher = messagePattern.matcher(line.trim());
            if (matcher.matches() && matcher.groupCount() == 3)
            {
                if (errorFile != null)
                {
                    JavacError error = new JavacError(errorFile, lineNum,
                            messageBuilder.toString().trim());
                    result.add(error);
                }
                errorFile = new File(matcher.group(1));
                lineNum = Integer.parseInt(matcher.group(2));
                messageBuilder = new StringBuilder().append(matcher.group(3));
                messageBuilder.append(Util.NL);
            }
            else
            {
                if (errorCountPattern.matcher(line).matches()
                        || !iter.hasNext())
                {
                    if (messageBuilder.length() != 0)
                    {
                        JavacError error = new JavacError(errorFile, lineNum,
                                messageBuilder.toString().trim());
                        result.add(error);
                    }
                }
                else if (!line.trim().equals("^")
                        && !noncheckerPattern.matcher(line).matches())
                {
                    messageBuilder.append(line);
                    messageBuilder.append(Util.NL);
                }
            }
        }

        // filter out for errors/warnings matching a regex
        String filterRegex = CheckerPlugin.getDefault().getPreferenceStore()
                .getString(CheckerPreferences.PREF_CHECKER_ERROR_FILTER_REGEX);
        if (!filterRegex.isEmpty())
        {
            Iterator<JavacError> errorIter = result.iterator();
            while (errorIter.hasNext())
            {
                JavacError err = errorIter.next();
                Matcher filterMatcher = Pattern.compile(filterRegex,
                        Pattern.DOTALL).matcher(err.message);
                if (filterMatcher.matches())
                {
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
    private static boolean handleErrors(String line)
    {
        StatusManager manager = StatusManager.getManager();

        // special case for missing checkers.jar (or processor class)
        Matcher procMatcher = noProcessorPattern.matcher(line);
        if (procMatcher.matches())
        {
            CheckerErrorStatus status;

            if (procMatcher.group(1).equals("''"))
            {
                status = new CheckerErrorStatus(
                        "No checkers configured. Try configuring checkers to use in the plugin preferences.");
            }
            else
            {
                status = new CheckerErrorStatus(
                        "Checker processor "
                                + procMatcher.group(1)
                                + " could not be found. Try adding checkers.jar to your project build path.");
            }
            manager.handle(status, StatusManager.SHOW);
            return false;
        }

        // Misc errors that prevent compiler from running:
        Matcher flagMatcher = invalidFlagPattern.matcher(line);
        if (flagMatcher.matches())
        {
            manager.handle(new CheckerErrorStatus("Invalid compiler flag: "
                    + flagMatcher.group(1)
                    + ". Check your preferences for invalid flags."),
                    StatusManager.SHOW);
            return false;
        }

        Matcher missingFileMatcher = missingFilePattern.matcher(line);
        if (missingFileMatcher.matches())
        {
            manager.handle(new CheckerErrorStatus("Cannot find file: "
                    + missingFileMatcher.group(1)
                    + ". You may have malformed input in your preferences."),
                    StatusManager.SHOW);
            return false;
        }

        return true;
    }
}
