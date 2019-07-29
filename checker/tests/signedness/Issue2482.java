import org.checkerframework.checker.signedness.qual.*;

class Issue2482 {

    void m1(String s, int a) {

        int len = s.length();

        len += a;
        len -= a;
        len /= a;
        len *= a;
    }

    void m2(byte[] b, int a) {

        int len = b.length;

        len += a;
        len -= a;
        len /= a;
        len *= a;
    }

    void m3(int a) {

        int len = -1; // Negative
        int len2 = 1; // Positive

        len += a;
        len -= a;
        len /= a;
        len *= a;

        len2 += a;
        len2 -= a;
        len2 /= a;
        len2 *= a;
    }
}
