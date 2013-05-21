package java.lang;

import checkers.nullness.quals.Nullable;

public class ClassCircularityError extends LinkageError {
    private static final long serialVersionUID = 0L;
    public ClassCircularityError() {
	super();
    }

    public ClassCircularityError(@Nullable String s) {
	super(s);
    }
}
