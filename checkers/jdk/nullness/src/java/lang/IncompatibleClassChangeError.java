package java.lang;

import checkers.nullness.quals.Nullable;

public
class IncompatibleClassChangeError extends LinkageError {
    private static final long serialVersionUID = 0L;
    public IncompatibleClassChangeError () {
	super();
    }

    public IncompatibleClassChangeError(@Nullable String s) {
	super(s);
    }
}
