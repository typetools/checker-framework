class StringSameLen {
    public void m(String s) {
        String t = s;

        for (int i = 0; i < s.length(); ++i) {
            char c = t.charAt(i);
        }
    }

    public void m2(String s) {
        String t = s.toString();

        for (int i = 0; i < s.length(); ++i) {
            char c = t.charAt(i);
        }
    }

    public void m4(String s) {
        char[] t = s.toCharArray();

        for (int i = 0; i < s.length(); ++i) {
            char c = t[i];
        }
    }

    public void m6(char[] s) {
        String t = String.valueOf(s);

        for (int i = 0; i < s.length; ++i) {
            char c = t.charAt(i);
        }
    }

    public void m7(char[] s) {
        String t = String.copyValueOf(s);

        for (int i = 0; i < s.length; ++i) {
            char c = t.charAt(i);
        }
    }

    public void m8(String s) {
        String t = s.intern();

        for (int i = 0; i < s.length(); ++i) {
            char c = t.charAt(i);
        }
    }

    public void constructor(String s) {
        String t = new String(new char[] {'a'});
    }
}
