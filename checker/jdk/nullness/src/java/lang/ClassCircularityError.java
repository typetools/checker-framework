package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClassCircularityError extends LinkageError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public ClassCircularityError() {
        super();
    }

    @SideEffectFree
    public ClassCircularityError(@Nullable String s) {
        super(s);
    }
}
