package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

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
