package java.lang;
import org.checkerframework.checker.javari.qual.*;

public class NullPointerException extends RuntimeException {
    private static final long serialVersionUID = 5162710183389028792L;

    public NullPointerException() {
        throw new RuntimeException("skeleton method");
    }

    public NullPointerException(String s) {
        throw new RuntimeException("skeleton method");
    }
}
