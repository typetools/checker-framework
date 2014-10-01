package java.text;
import org.checkerframework.checker.javari.qual.*;

public class ParseException extends Exception {
    // Added to avoid a warning
    private static final long serialVersionUID = 0;

    public ParseException(String s, int errorOffset) {
        throw new RuntimeException("skeleton method");
    }

    public int getErrorOffset (@ReadOnly ParseException this) {
        throw new RuntimeException("skeleton method");
    }
}
