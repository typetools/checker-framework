package java.lang;
import org.checkerframework.checker.javari.qual.*;

public class IllegalStateException extends RuntimeException {
    public IllegalStateException() {
        throw new RuntimeException("skeleton method");
    }

    public IllegalStateException(String s) {
        throw new RuntimeException("skeleton method");
    }

    public IllegalStateException(String message, Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    public IllegalStateException(Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    static final long serialVersionUID = -1848914673093119416L;
}
