public class CharCast {

    void clientReturnedCastedShort(short s) {
        int x = s;
        // :: warning: (cast.unsafe)
        char c = (char) x;
    }

    void clientCastShort(int i) {
        int x = (short) i;
        // :: warning: (cast.unsafe)
        char c = (char) x;
    }

    void clientReturnedCastedShortLiteral(short s) {
        int x = s;
        // :: warning: (cast.unsafe)
        char c = (char) x;
    }

    void clientCastShortLiteral() {
        int x = (short) 1;
        char c = (char) x;
    }

    void clientShortUpcast() {
        short x = 1;
        int y = x;
        char c = (char) y;
    }

    void clientReturnedShortUpcast(short s) {
        int y = s;
        // :: warning: (cast.unsafe)
        char c = (char) y;
    }

    short returnShort() {
        short x = 1;
        return x;
    }

    short returnCastedShort(int i) {
        return (short) i;
    }

    short returnCastedShortLiteral() {
        return (short) 1;
    }
}
