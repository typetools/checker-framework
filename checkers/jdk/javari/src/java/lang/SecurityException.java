package java.lang;
import checkers.javari.quals.*;

public class SecurityException extends RuntimeException {
    private static final long serialVersionUID = 6878364983674394167L;

    public SecurityException() {
        throw new RuntimeException("skeleton method");
    }

    public SecurityException(String s) {
        throw new RuntimeException("skeleton method");
    }

    public SecurityException(String message, Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    public SecurityException(Throwable cause) {
        throw new RuntimeException("skeleton method");
    }
}
