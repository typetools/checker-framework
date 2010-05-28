package checkers.eclipse.javac;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import checkers.eclipse.util.*;

/**
 * Error reported by javac. Created by parsing javac output.
 */
public class JavacError {
    private static final boolean VERBOSE = true;
    public final File file;
    public final int lineNumber;
    public final String message;

    public JavacError(File file, int lineNumber, String message) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.message = message;
    }

    @Override
    public String toString() {
        return file.getPath() + ":" + lineNumber + ": " + message;
    }

    /**
     * Parses javac output and converts to a list of errors.
     */
    // XXX very nasty code - needs cleanup
    private static final Pattern errorCountPattern = Pattern
            .compile("^[0-9]+ errors*$");

    public static List<JavacError> parse(String javacoutput) {
        if (VERBOSE)
            System.out.println("javac output:\n" + javacoutput);
        if (javacoutput == null)
            return null;
        List<JavacError> result = new ArrayList<JavacError>();
        List<String> lines = Arrays.asList(javacoutput.split(Util.NL));
        Iterator<String> iter = lines.iterator();
        if (!iter.hasNext())
            return result;
        String line = iter.next();
        do {
            String[] segments = line.split(":");
            if (segments.length != 3) {
                if (iter.hasNext())
                    line = iter.next();
                continue; // ??
            }
            try {
                int lineNumber = Integer.parseInt(segments[1]);
                StringBuilder msg = new StringBuilder();
                msg.append(segments[2].trim());
                boolean foundNextEntry = false;
                while (!foundNextEntry && iter.hasNext()) {
                    line = iter.next();
                    foundNextEntry = line.split(":").length == 3
                            && new File(line.split(":")[0]).exists();
                    if (!foundNextEntry
                            && !errorCountPattern.matcher(line).matches()
                            && !line.trim().equals("^")) {
                        msg.append(Util.NL).append(line);
                    }
                }
                File f = new File(segments[0]);
                result.add(new JavacError(f, lineNumber, msg.toString()));
            } catch (NumberFormatException e) {
                if (iter.hasNext())
                    line = iter.next();
                continue;
            }
        } while (iter.hasNext());
        return result;
    }
}
