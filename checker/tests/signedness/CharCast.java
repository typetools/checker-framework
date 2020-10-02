public class CharCast {

    void m1(short s) {
        int x = s;
        // :: warning: (cast.unsafe)
        char c = (char) x;
    }

    void m2(int i) {
        int x = (short) i;
        // :: warning: (cast.unsafe)
        char c = (char) x;
    }

    void m3() {
        int x = (short) 1;
        char c = (char) x;
    }

    void m4() {
        short x = 1;
        int y = x;
        char c = (char) y;
    }
}
