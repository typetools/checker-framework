package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class UnsatisfiedLinkError extends LinkageError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public UnsatisfiedLinkError() {
        super();
    }

    @SideEffectFree
    public UnsatisfiedLinkError(@Nullable String s) {
        super(s);
    }
}
