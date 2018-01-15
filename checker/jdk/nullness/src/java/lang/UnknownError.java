package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class UnknownError extends VirtualMachineError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public UnknownError() {
        super();
    }

    @SideEffectFree
    public UnknownError(@Nullable String s) {
        super(s);
    }
}
