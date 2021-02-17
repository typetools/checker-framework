public class Issue2482 {

    void regularAssignment(byte[] b, int c) {
        int a = b.length;
        a = a + c;
    }

    void compoundAssignment(byte[] b, int c) {
        int a = b.length;
        a += c;
    }

    void stringLenAdd(String s, int a) {
        int len = s.length();
        len += a;
    }

    void stringLenSub(String s, int a) {
        int len = s.length();
        len -= a;
    }

    void stringLenDiv(String s, int a) {
        int len = s.length();
        len /= a;
    }

    void stringLenMul(String s, int a) {
        int len = s.length();
        len *= a;
    }

    void arrayLenAdd(byte[] b, int a) {
        int len = b.length;
        len += a;
    }

    void arrayLenSub(byte[] b, int a) {
        int len = b.length;
        len -= a;
    }

    void arrayLenDiv(byte[] b, int a) {
        int len = b.length;
        len /= a;
    }

    void arrayLenMul(byte[] b, int a) {
        int len = b.length;
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
