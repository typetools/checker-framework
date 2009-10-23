package java.lang;

import checkers.nullness.quals.*;

public
class UnknownError extends VirtualMachineError {
    public UnknownError() {
	super();
    }

    public UnknownError(@Nullable String s) {
	super(s);
    }
}
