package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class NoClassDefFoundError extends LinkageError {
  private static final long serialVersionUID = 0;
    @SideEffectFree
    public NoClassDefFoundError() {
        super();
    }

    @SideEffectFree
    public NoClassDefFoundError(@Nullable String s) {
        super(s);
    }
}
