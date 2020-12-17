public class FlowCompoundConcatenation {
    static String getNonNullString() {
        return "";
    }

    public static void testCompoundAssignWithNullAndMethodCall() {
        String s = null;
        s += getNonNullString();
        s.toString();
    }

    public static void testCompoundAssignWithNull() {
        String s = null;
        s += "hello";
        s.toString();
    }
}
