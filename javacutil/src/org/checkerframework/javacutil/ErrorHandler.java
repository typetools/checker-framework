package org.checkerframework.javacutil;

/**
 * An implementation of the ErrorHandler interface can be registered with the ErrorReporter class to
 * change the default behavior on errors.
 */
public interface ErrorHandler {

    /**
     * Log an error message and abort processing.
     *
     * @param msg the error message to log
     */
    void errorAbort(String msg);

    void errorAbort(String msg, Throwable cause);

    /**
     * Log a warning.
     *
     * @param format format string
     * @param args arguments to the format string
     */
    void warn(String format, Object... args);

    /**
     * If the warning has not been logged before, log it.
     *
     * @param format format string
     * @param args arguments to the format string
     */
    void warnOnce(String format, Object... args);
}
