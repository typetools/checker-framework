import org.checkerframework.checker.signedness.qual.*;

public class WideningConversion2 {

    @Signed byte b1;
    @Signed byte b2;
    @Unsigned byte ub1;
    @Unsigned byte ub2;

    char c1;
    char c2;

    @Signed int si2;
    @SignedPositive int pi2;

    void plus() {
        @Signed int si;
        @SignedPositive int pi;
        si = c1;
        pi = c1;
        si = (int) c1;
        si = pi2;
        si = (int) pi2;
        pi = (int) c1;
        si = (int) c1 + (int) c2;
        si = (int) (c1 + c2);
        si = c1 + c2;

        char c;
        c = (char) (c1 + c2);

        @Signed byte b;
        b = (byte) (b1 + b2);
        @Unsigned byte ub;
        ub = (byte) (ub1 + ub2);
        // char c;
        // c = (char) (c1 + si2);
    }
}
