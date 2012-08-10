package java.lang;

import checkers.nonnull.quals.Nullable;

public
class UnknownError extends VirtualMachineError {
    private static final long serialVersionUID = 0L;
    public UnknownError() {
	super();
    }

    public UnknownError(@Nullable String s) {
	super(s);
    }
}
