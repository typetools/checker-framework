package java.lang;

import checkers.nullness.quals.*;

public
class StackOverflowError extends VirtualMachineError {
    public StackOverflowError() {
	super();
    }

    public StackOverflowError(@Nullable String s) {
	super(s);
    }
}
