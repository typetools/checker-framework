package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class ClassFormatError extends LinkageError {
  private static final long serialVersionUID = 0;
    @SideEffectFree
    public ClassFormatError() {
        super();
    }

    @SideEffectFree
    public ClassFormatError(@Nullable String s) {
        super(s);
    }
}
