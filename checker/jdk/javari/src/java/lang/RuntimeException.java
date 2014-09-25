package java.lang;
import org.checkerframework.checker.javari.qual.*;

public class RuntimeException extends Exception {
    static final long serialVersionUID = -7034897190745766939L;

    public RuntimeException() {
        throw new RuntimeException("skeleton method");
    }

    public RuntimeException(String message) {
        throw new RuntimeException("skeleton method");
    }

    public RuntimeException(String message, Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    public RuntimeException(Throwable cause) {
        throw new RuntimeException("skeleton method");
    }
}
