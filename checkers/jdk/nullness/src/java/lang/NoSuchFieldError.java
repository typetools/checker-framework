package java.lang;

import checkers.nullness.quals.*;

public
class NoSuchFieldError extends IncompatibleClassChangeError {
    public NoSuchFieldError() {
	super();
    }

    public NoSuchFieldError(@Nullable String s) {
	super(s);
    }
}
