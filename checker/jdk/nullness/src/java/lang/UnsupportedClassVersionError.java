package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class UnsupportedClassVersionError extends ClassFormatError {
    private static final long serialVersionUID = 0L;
    @SideEffectFree
    public UnsupportedClassVersionError() {
        super();
    }

    @SideEffectFree
    public UnsupportedClassVersionError(@Nullable String s) {
        super(s);
    }
}
