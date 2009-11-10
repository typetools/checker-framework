package java.lang;

import checkers.nullness.quals.*;

public
class UnsupportedClassVersionError extends ClassFormatError {
    private static final long serialVersionUID = 0L;
    public UnsupportedClassVersionError() {
	super();
    }

    public UnsupportedClassVersionError(@Nullable String s) {
	super(s);
    }
}
