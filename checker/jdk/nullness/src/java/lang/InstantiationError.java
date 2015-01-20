package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

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
