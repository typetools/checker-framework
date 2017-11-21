package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class OutOfMemoryError extends VirtualMachineError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public OutOfMemoryError() {
        super();
    }

    @SideEffectFree
    public OutOfMemoryError(@Nullable String s) {
        super(s);
    }
}
