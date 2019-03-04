package org.checkerframework.javacutil;

/** Exception type indicating a bug in the framework or in a checker implementation. */
@SuppressWarnings("serial")
public class BugInCF extends RuntimeException {

    /**
     * Constructs a new CheckerError with the specified detail message and no cause (use this at the
     * root cause).
     *
     * @param message the detail message
     */
    public BugInCF(String message) {
        this(message, new Throwable());
    }

    /**
     * Constructs a new CheckerError with a detail message composed from the given arguments, and
     * with no cause (use this at the root cause).
     *
     * @param fmt the format string
     * @param args the arguments for the format string
     */
    public BugInCF(String fmt, Object... args) {
        this(String.format(fmt, args), new Throwable());
    }

    /**
     * Constructs a new CheckerError with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BugInCF(String message, Throwable cause) {
        super(message, cause);
        if (message == null) {
            throw new BugInCF("Must have a detail message.");
        }
        if (cause == null) {
            throw new BugInCF("Must have a cause throwable.");
        }
    }
}
