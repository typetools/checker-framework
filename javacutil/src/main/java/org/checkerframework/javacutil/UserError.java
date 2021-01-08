package org.checkerframework.javacutil;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception type indicating a mistake by an end user in using the Checker Framework, such as
 * incorrect command-line arguments.
 */
@SuppressWarnings("serial")
public class UserError extends RuntimeException {

    /**
     * Constructs a new CheckerError with the specified detail message.
     *
     * @param message the detail message
     */
    public UserError(String message) {
        super(message);
        if (message == null) {
            throw new Error("Must have a detail message.");
        }
    }

    /**
     * Constructs a new CheckerError with a detail message composed from the given arguments.
     *
     * @param fmt the format string
     * @param args the arguments for the format string
     */
    public UserError(String fmt, @Nullable Object... args) {
        this(String.format(fmt, args));
    }
}
