package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class StackOverflowError extends VirtualMachineError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public StackOverflowError() {
        super();
    }

    @SideEffectFree
    public StackOverflowError(@Nullable String s) {
        super(s);
    }
}
