package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class AbstractMethodError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public AbstractMethodError() {
        super();
    }

    @SideEffectFree
    public AbstractMethodError(@Nullable String s) {
        super(s);
    }
}
