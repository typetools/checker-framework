import org.checkerframework.checker.signedness.qual.*;

public class WideningConversion3 {

    char c1;

    void plus() {
        @Signed int si;
        si = c1;
        si = (int) c1;
    }
}
