class Ternary {
    void m1(boolean b) {
        String s = b ? new String("foo") : null;
    }

    void m2(boolean b) {
        String s = b ? null : new String("foo");
    }

    String m3(boolean b) {
        return b ? new String("foo") : null;
    }

    void m4(boolean b) {
        String[] s = b ? new String[5] : null;
    }

    void m5(boolean b) {
        Object o = new Object();
        String s = b ? (String) o : null;
    }

    void m6(boolean b) {
        String p = "x*(((";
        String s = b ? p : null;
    }
}