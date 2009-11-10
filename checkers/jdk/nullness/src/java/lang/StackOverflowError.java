package java.lang;

import checkers.nullness.quals.*;

public
class StackOverflowError extends VirtualMachineError {
    private static final long serialVersionUID = 0L;
    public StackOverflowError() {
	super();
    }

    public StackOverflowError(@Nullable String s) {
	super(s);
    }
}
