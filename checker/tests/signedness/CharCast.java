public class CharCast {
    int i;

    void clientReturnedCastedShort1(int x) {
        // :: warning: (cast.unsafe)
        char c = (char) x;
    }

    void clientReturnedCastedShort2(int x) {
        // :: warning: (cast.unsafe)
        String s = Character.toString((char) x);
    }

    void clientReturnedCastedShort() {
        int x = returnCastedShort();
        // :: warning: (cast.unsafe)
        String s = Character.toString((char) x);
    }

    short returnCastedShort() {
        return (short) i;
    }

    void clientCastShort() {
        int x = (short) i;
        // :: warning: (cast.unsafe)
        String s = Character.toString((char) x);
    }

    void clientReturnedCastedShortLiteral() {
        int x = returnCastedShortLiteral();
        // :: warning: (cast.unsafe)
        String s = Character.toString((char) x);
    }

    short returnCastedShortLiteral() {
        return (short) 1;
    }

    void clientCastShortLiteral() {
        int x = (short) 1;
        String s = Character.toString((char) x);
    }

    void clientShortUpcast() {
        short x = 1;
        int y = x;
        String s = Character.toString((char) y);
    }

    void clientReturnedShortUpcast() {
        int y = returnShort();
        // :: warning: (cast.unsafe)
        String s = Character.toString((char) y);
    }

    short returnShort() {
        short x = 1;
        return x;
    }
}
