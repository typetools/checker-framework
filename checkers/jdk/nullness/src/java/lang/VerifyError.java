package java.lang;

import checkers.nonnull.quals.Nullable;

public
class VerifyError extends LinkageError {
    private static final long serialVersionUID = 0L;
    public VerifyError() {
	super();
    }

    public VerifyError(@Nullable String s) {
	super(s);
    }
}
