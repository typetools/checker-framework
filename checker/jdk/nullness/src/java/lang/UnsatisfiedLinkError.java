package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

public
class UnsatisfiedLinkError extends LinkageError {
    private static final long serialVersionUID = 0L;
    public UnsatisfiedLinkError() {
        super();
    }

    public UnsatisfiedLinkError(@Nullable String s) {
        super(s);
    }
}
