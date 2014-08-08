package java.lang;
import org.checkerframework.checker.javari.qual.*;

public class IllegalArgumentException extends RuntimeException {
    public IllegalArgumentException() {
        throw new RuntimeException("skeleton method");
    }

    public IllegalArgumentException(String s) {
        throw new RuntimeException("skeleton method");
    }

    public IllegalArgumentException(String message, Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    public IllegalArgumentException(Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    private static final long serialVersionUID = -5365630128856068164L;
}
