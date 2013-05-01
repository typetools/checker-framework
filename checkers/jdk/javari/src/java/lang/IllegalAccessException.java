package java.lang;
import checkers.javari.quals.*;

// In openjdk7 this class extends ReflectiveOperationException.
public class IllegalAccessException extends Exception {
    private static final long serialVersionUID = 6616958222490762034L;

    public IllegalAccessException() {
        throw new RuntimeException("skeleton method");
    }

    public IllegalAccessException(String s) {
        throw new RuntimeException("skeleton method");
    }
}
