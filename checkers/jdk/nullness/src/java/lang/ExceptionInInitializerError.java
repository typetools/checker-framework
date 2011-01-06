package java.lang;

import checkers.nullness.quals.*;

public class ExceptionInInitializerError extends LinkageError {
    private static final long serialVersionUID = 1521711792217232256L;

    private Throwable exception;

    public ExceptionInInitializerError() {
        initCause(null);  // Disallow subsequent initCause
    }

    public ExceptionInInitializerError(@Nullable Throwable thrown) {
        initCause(null);  // Disallow subsequent initCause
	this.exception = thrown;
    }

    public ExceptionInInitializerError(@Nullable String s) {
	super(s);
        initCause(null);  // Disallow subsequent initCause
    }

    public @Nullable Throwable getException() {
	return exception;
    }

    public @Nullable Throwable getCause() {
	return exception;
    }
}
