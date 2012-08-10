package java.lang;

import checkers.nonnull.quals.Nullable;

public
class NoSuchFieldError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    public NoSuchFieldError() {
	super();
    }

    public NoSuchFieldError(@Nullable String s) {
	super(s);
    }
}
