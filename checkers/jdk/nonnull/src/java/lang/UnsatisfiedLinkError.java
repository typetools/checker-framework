package java.lang;

import checkers.nonnull.quals.Nullable;

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
