package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class InstantiationError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public InstantiationError() {
        super();
    }

    @SideEffectFree
    public InstantiationError(@Nullable String s) {
        super(s);
    }
}
