package java.lang;

import checkers.nullness.quals.*;

public
class VerifyError extends LinkageError {
    public VerifyError() {
	super();
    }

    public VerifyError(@Nullable String s) {
	super(s);
    }
}
