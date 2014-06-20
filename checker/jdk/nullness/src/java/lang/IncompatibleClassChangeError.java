package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

public
class IncompatibleClassChangeError extends LinkageError {
    private static final long serialVersionUID = 0L;
    public IncompatibleClassChangeError () {
	super();
    }

    public IncompatibleClassChangeError(@Nullable String s) {
	super(s);
    }
}
