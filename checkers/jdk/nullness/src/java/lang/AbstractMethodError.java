package java.lang;

import checkers.nullness.quals.Nullable;

public
class AbstractMethodError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    public AbstractMethodError() {
	super();
    }

    public AbstractMethodError(@Nullable String s) {
	super(s);
    }
}
