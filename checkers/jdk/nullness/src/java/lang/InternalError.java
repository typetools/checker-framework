package java.lang;

import checkers.nullness.quals.Nullable;

public
class InternalError extends VirtualMachineError {
    private static final long serialVersionUID = 0L;
    public InternalError() {
	super();
    }

    public InternalError(@Nullable String s) {
	super(s);
    }
}
