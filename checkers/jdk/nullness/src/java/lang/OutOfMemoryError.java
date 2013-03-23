package java.lang;

import checkers.nullness.quals.Nullable;

public
class OutOfMemoryError extends VirtualMachineError {
    private static final long serialVersionUID = 0L;
    public OutOfMemoryError() {
        super();
    }

    public OutOfMemoryError(@Nullable String s) {
        super(s);
    }
}
