package java.lang;
import org.checkerframework.checker.javari.qual.*;

public class NumberFormatException extends IllegalArgumentException {
    static final long serialVersionUID = -2848938806368998894L;

    public NumberFormatException () {
        throw new RuntimeException("skeleton method");
    }

    public NumberFormatException (String s) {
        throw new RuntimeException("skeleton method");
    }

    static NumberFormatException forInputString(String s) {
        throw new RuntimeException("skeleton method");
    }
}
