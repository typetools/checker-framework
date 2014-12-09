package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

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
