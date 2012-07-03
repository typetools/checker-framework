package java.lang;

import checkers.nullness.quals.Nullable;

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
