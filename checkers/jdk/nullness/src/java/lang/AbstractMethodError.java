package java.lang;

import checkers.nullness.quals.*;

public
class AbstractMethodError extends IncompatibleClassChangeError {
    public AbstractMethodError() {
	super();
    }

    public AbstractMethodError(@Nullable String s) {
	super(s);
    }
}
