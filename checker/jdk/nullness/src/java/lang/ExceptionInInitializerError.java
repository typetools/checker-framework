package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ExceptionInInitializerError extends LinkageError {
    private static final long serialVersionUID = 1521711792217232256L;

    private Throwable exception;

    @SideEffectFree
    public ExceptionInInitializerError() {
        initCause(null);  // Disallow subsequent initCause
    }

    @SideEffectFree
    public ExceptionInInitializerError(@Nullable Throwable thrown) {
        initCause(null);  // Disallow subsequent initCause
        this.exception = thrown;
    }

    @SideEffectFree
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
