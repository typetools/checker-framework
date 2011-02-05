package java.lang;

import checkers.nullness.quals.*;

public
class NoClassDefFoundError extends LinkageError {
  private static final long serialVersionUID = 0;
    public NoClassDefFoundError() {
	super();
    }

    public NoClassDefFoundError(@Nullable String s) {
	super(s);
    }
}
