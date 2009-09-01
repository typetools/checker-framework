package java.lang;

import checkers.nullness.quals.*;

public
class UnsupportedClassVersionError extends ClassFormatError {
    public UnsupportedClassVersionError() {
	super();
    }

    public UnsupportedClassVersionError(@Nullable String s) {
	super(s);
    }
}
