package java.lang;

import checkers.nullness.quals.*;

public
class InstantiationError extends IncompatibleClassChangeError {
    public InstantiationError() {
	super();
    }

    public InstantiationError(@Nullable String s) {
	super(s);
    }
}
