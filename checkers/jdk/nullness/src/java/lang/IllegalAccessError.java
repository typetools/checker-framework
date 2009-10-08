package java.lang;

import checkers.nullness.quals.*;

public class IllegalAccessError extends IncompatibleClassChangeError {
    public IllegalAccessError() {
	super();
    }

    public IllegalAccessError(@Nullable String s) {
	super(s);
    }
}
