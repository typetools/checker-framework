package java.text;
import checkers.javari.quals.*;

public class ParseException extends Exception {
    public ParseException(String s, int errorOffset) {
        throw new RuntimeException("skeleton method");
    }

    public int getErrorOffset () @ReadOnly {
        throw new RuntimeException("skeleton method");
    }
}
