package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

public
class NoSuchMethodError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    public NoSuchMethodError() {
        super();
    }

    public NoSuchMethodError(@Nullable String s) {
        super(s);
    }
}
