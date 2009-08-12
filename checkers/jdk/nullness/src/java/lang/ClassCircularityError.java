package java.lang;

import checkers.nullness.quals.*;

public class ClassCircularityError extends LinkageError {
    public ClassCircularityError() {
	super();
    }

    public ClassCircularityError(@Nullable String s) {
	super(s);
    }
}
