package java.lang;

import checkers.nullness.quals.*;

public
class ClassFormatError extends LinkageError {
  private static final long serialVersionUID = 0;
    public ClassFormatError() {
	super();
    }

    public ClassFormatError(@Nullable String s) {
	super(s);
    }
}
