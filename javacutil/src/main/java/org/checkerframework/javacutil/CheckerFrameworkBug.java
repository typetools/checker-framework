package org.checkerframework.javacutil;

/** Exception type indicating a bug in the framework or in a checker implementation. */
@SuppressWarnings("serial")
public class CheckerFrameworkBug extends RuntimeException {

    /**
     * Constructs a new CheckerError with the specified detail message and no cause (use this at the
     * root cause).
     *
     * @param message the detail message
     */
    public CheckerFrameworkBug(String msg) {
        this(msg, (Throwable) null);
    }

    /**
     * Constructs a new CheckerError with a detail message composed from the given arguments, and
     * with no cause (use this at the root cause).
     *
     * @param message the detail message
     */
    public CheckerFrameworkBug(String fmt, Object... args) {
        this(String.format(fmt, args));
    }

    /**
     * Constructs a new CheckerError with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause, or null
     */
    public CheckerFrameworkBug(String msg, Throwable cause) {
        super(msg, cause);
        if (msg == null) {
            throw new Error("Must have a detail message.");
        }
    }
}
