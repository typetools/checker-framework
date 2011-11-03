package java.io;

import checkers.nullness.quals.*;

public class IOException extends Exception {
    private static final long serialVersionUID = 0L;
    public IOException() { throw new RuntimeException("skeleton method"); }
    public IOException(@Nullable String message) { throw new RuntimeException("skeleton method"); }
    public IOException(@Nullable String message, @Nullable Throwable cause) { throw new RuntimeException("skeleton method"); }
    public IOException(@Nullable Throwable cause) { throw new RuntimeException("skeleton method"); }
}
