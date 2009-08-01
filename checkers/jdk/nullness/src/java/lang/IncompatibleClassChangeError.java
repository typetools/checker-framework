package java.lang;

import checkers.nullness.quals.*;

public
class IncompatibleClassChangeError extends LinkageError {
    public IncompatibleClassChangeError () {
	super();
    }

    public IncompatibleClassChangeError(@Nullable String s) {
	super(s);
    }
}
