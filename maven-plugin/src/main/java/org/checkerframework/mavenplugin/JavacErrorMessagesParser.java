package org.checkerframework.mavenplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

/**
* Parser for errors emitted by {@code javac}. This is derived from the
* {@link JavacCompiler#parseModernStream} and
* {@link JavacCompiler#parseModernError} methods of {@link JavacCompiler}.
*/
public class JavacErrorMessagesParser {
    protected static final String EOL = System.getProperty("line.separator");

    private static final String ERROR_PREFIX = "error: ";
    private static final String WARNING_PREFIX = "warning: ";

    private JavacErrorMessagesParser() { /* Prevent instantiation */ }

    /**
    * Parses the compiler error output into a list of {@code CompilerError} objects.
    *
    * @param input the compiler error output
    * @return the parsed error output
    */

    public static List<CompilerError> parseMessages(String input) {
        List<CompilerError> errors = new ArrayList<CompilerError>();

        try {
        final BufferedReader reader = new BufferedReader(new StringReader(input));
        try {
            final StringBuilder errorBuffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                // Skip summary of warnings/errors
                if (Character.isDigit(line.charAt(0))) {
                    continue;
                }

                // Start of a new message?
                if (errorBuffer.length() > 0 && !Character.isWhitespace(line.charAt(0))) {
                    errors.add(parseMessage(errorBuffer.toString()));
                    errorBuffer.setLength(0);
                }
                errorBuffer.append(line).append(EOL);
            }

            // Handle the last message
            if (errorBuffer.length() > 0) {
                errors.add(parseMessage(errorBuffer.toString()));
            }

            return errors;
        } finally {
            reader.close();
        }
    } catch (IOException e) {
        // Should never happen
        throw new RuntimeException(e);
    }
}

    /**
     * Parses a single error message.
     *
     * @param message the error message
     * @return the parsed error message
     */
    public static CompilerError parseMessage(String message) {
        final StringBuilder buffer;
        final StringTokenizer tokens = new StringTokenizer(message, ":");

        boolean isError = true;
        try {
            String file = tokens.nextToken();

            // TODO: Figure out why this exists in the original JavacCompiler implementation
            if (file.length() == 1) {
                file = new StringBuilder(file).append(":").append(tokens.nextToken()).toString();
            }

            final int line = Integer.parseInt(tokens.nextToken());

            buffer = new StringBuilder();

            String description = tokens.nextToken(EOL).substring(2);
            if (description.startsWith(ERROR_PREFIX)) {
                description = description.substring(ERROR_PREFIX.length());
            } else if (description.startsWith(WARNING_PREFIX)) {
                isError = false;
                description = description.substring(WARNING_PREFIX.length());
            }
            buffer.append(description);

            final String context = tokens.nextToken(EOL);
            final String pointer = tokens.nextToken(EOL);
            final int startColumn = pointer.indexOf("^");
            int endColumn = context == null ? startColumn : context.indexOf(" ", startColumn);
            if (endColumn == -1) {
                endColumn = context.length();
            }

                // Extra context
            if (tokens.hasMoreTokens()) {
                do {
                    buffer.append(EOL).append(tokens.nextToken(EOL));
                } while (tokens.hasMoreTokens());
            }

            return new CompilerError(file, isError, line, startColumn, line, endColumn, buffer.toString());
        } catch (NoSuchElementException e) {
            return new CompilerError("no more tokens - could not parse error message: " + message, isError);
        } catch (NumberFormatException e) {
            return new CompilerError("could not parse error message: " + message, isError);
        } catch (Exception e) {
            return new CompilerError("could not parse error message: " + message, isError);
        }
    }
}