package java.lang;

import checkers.nullness.quals.*;

public
class NoSuchMethodError extends IncompatibleClassChangeError {
    public NoSuchMethodError() {
	super();
    }

    public NoSuchMethodError(@Nullable String s) {
	super(s);
    }
}
