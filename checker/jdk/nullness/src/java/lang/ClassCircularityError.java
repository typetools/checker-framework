package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

public class ClassCircularityError extends LinkageError {
    private static final long serialVersionUID = 0L;
    public ClassCircularityError() {
	super();
    }

    public ClassCircularityError(@Nullable String s) {
	super(s);
    }
}
