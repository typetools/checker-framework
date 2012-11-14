package java.lang;

import checkers.nullness.quals.*;

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
