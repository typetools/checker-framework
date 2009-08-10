package java.lang;

import checkers.nullness.quals.*;

public
class OutOfMemoryError extends VirtualMachineError {
    public OutOfMemoryError() {
	super();
    }

    public OutOfMemoryError(@Nullable String s) {
	super(s);
    }
}
