package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class NoSuchMethodError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public NoSuchMethodError() {
        super();
    }

    @SideEffectFree
    public NoSuchMethodError(@Nullable String s) {
        super(s);
    }
}
