package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

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
