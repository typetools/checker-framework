package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public class IllegalAccessError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public IllegalAccessError() {
        super();
    }

    @SideEffectFree
    public IllegalAccessError(@Nullable String s) {
        super(s);
    }
}
