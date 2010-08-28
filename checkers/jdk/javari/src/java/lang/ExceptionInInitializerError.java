package java.lang;
import checkers.javari.quals.*;

public class ExceptionInInitializerError extends LinkageError {
    private static final long serialVersionUID = 1521711792217232256L;

    private Throwable exception;

    public ExceptionInInitializerError() {
        throw new RuntimeException("skeleton method");
    }

    public ExceptionInInitializerError(Throwable thrown) {
        throw new RuntimeException("skeleton method");
    }

    public ExceptionInInitializerError(String s) {
        throw new RuntimeException("skeleton method");
    }

    public Throwable getException() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public Throwable getCause() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }
}
