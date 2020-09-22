import org.checkerframework.checker.signedness.qual.*;

public class WideningConversion3 {

    char c1;
    char c2;

    void plus() {
        @Signed int si;

        si = c1 + c2;

        si = (int) (c1 + c2);

        si = c1;
        si = (int) c1;
    }
}
