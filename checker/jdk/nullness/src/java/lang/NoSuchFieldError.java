package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class NoSuchFieldError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public NoSuchFieldError() {
        super();
    }

    @SideEffectFree
    public NoSuchFieldError(@Nullable String s) {
        super(s);
    }
}
