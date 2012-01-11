package java.lang;
import checkers.javari.quals.*;

public class NoClassDefFoundError extends LinkageError {
    private static final long serialVersionUID = 9095859863287012458L;

    public NoClassDefFoundError() {
        throw new RuntimeException("skeleton method");
    }

    public NoClassDefFoundError(String s) {
        throw new RuntimeException("skeleton method");
    }
}
