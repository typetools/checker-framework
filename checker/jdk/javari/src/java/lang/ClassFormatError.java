package java.lang;
import org.checkerframework.checker.javari.qual.*;

public class ClassFormatError extends LinkageError {
    private static final long serialVersionUID = 0L;
    public ClassFormatError() {
        throw new RuntimeException("skeleton method");
    }

    public ClassFormatError(String s) {
        throw new RuntimeException("skeleton method");
    }
}
