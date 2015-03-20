package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

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
