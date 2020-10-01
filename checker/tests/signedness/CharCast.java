public class CharCast {
    int i;

    void clientReturnedCastedShort1(int x) {
        // :: warning: (cast.unsafe)
        char c = (char) x;
    }

    void clientReturnedCastedShort2(int x) {
        // :: warning: (cast.unsafe)
        String str = Character.toString((char) x);
    }

    void clientReturnedCastedShort(short s) {
        int x = s;
        // :: warning: (cast.unsafe)
        String str = Character.toString((char) x);
    }

    void clientCastShort() {
        int x = (short) i;
        // :: warning: (cast.unsafe)
        String str = Character.toString((char) x);
    }

    void clientReturnedCastedShortLiteral(short s) {
        int x = s;
        // :: warning: (cast.unsafe)
        String str = Character.toString((char) x);
    }

    void clientCastShortLiteral() {
        int x = (short) 1;
        String str = Character.toString((char) x);
    }

    void clientShortUpcast() {
        short x = 1;
        int y = x;
        String str = Character.toString((char) y);
    }

    void clientReturnedShortUpcast(short s) {
        int y = s;
        // :: warning: (cast.unsafe)
        String str = Character.toString((char) y);
    }

    short returnShort() {
        short x = 1;
        return x;
    }

    short returnCastedShort() {
        return (short) i;
    }

    short returnCastedShortLiteral() {
        return (short) 1;
    }
}
