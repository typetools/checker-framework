package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

public
class NoClassDefFoundError extends LinkageError {
  private static final long serialVersionUID = 0;
    public NoClassDefFoundError() {
        super();
    }

    public NoClassDefFoundError(@Nullable String s) {
        super(s);
    }
}
