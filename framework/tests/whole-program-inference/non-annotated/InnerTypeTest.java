class InnerTypeTest {
    public static String toStringQuoted(Object[] a) {
        return toString(a, true);
    }

    public static String toString(Object[] a, boolean quoted) {
        if (a == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer();
        return sb.toString();
    }

    public void bar() {
        assert InnerTypeTest.toStringQuoted((Object[]) null).equals("null");
    }
}
