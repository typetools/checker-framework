package java.lang;
import checkers.javari.quals.*;

public class Error extends Throwable {
    static final long serialVersionUID = 4980196508277280342L;

    public Error() {
        throw new RuntimeException("skeleton method");
    }

    public Error(String message) {
        throw new RuntimeException("skeleton method");
    }

    public Error(String message, Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    public Error(Throwable cause) {
        throw new RuntimeException("skeleton method");
    }
}
