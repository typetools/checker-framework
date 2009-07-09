package java.lang;

import checkers.nullness.quals.*;

public
class InternalError extends VirtualMachineError {
    public InternalError() {
	super();
    }

    public InternalError(@Nullable String s) {
	super(s);
    }
}
