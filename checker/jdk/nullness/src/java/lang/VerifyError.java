package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class VerifyError extends LinkageError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public VerifyError() {
        super();
    }

    @SideEffectFree
    public VerifyError(@Nullable String s) {
        super(s);
    }
}
