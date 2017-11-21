package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public
class LinkageError extends Error {
  private static final long serialVersionUID = 0;
    @SideEffectFree
    public LinkageError() {
        super();
    }

    @SideEffectFree
    public LinkageError(@Nullable String s) {
        super(s);
    }
}
