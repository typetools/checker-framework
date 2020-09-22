import org.checkerframework.checker.signedness.qual.*;

public class WideningConversion3 {

    void shortToChar1(short s) {
        // :: warning: (cast.unsafe)
        char c = (char) s;
    }
}
