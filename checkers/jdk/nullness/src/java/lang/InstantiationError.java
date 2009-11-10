package java.lang;

import checkers.nullness.quals.*;

public
class InstantiationError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    public InstantiationError() {
	super();
    }

    public InstantiationError(@Nullable String s) {
	super(s);
    }
}
