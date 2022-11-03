// Test case based on a crash encountered when running WPI on plume-util's Intern.java.
// The crash was caused by an AnnotatedDeclaredType being unsafely cast to an AnnotatedArrayType
// during WPI.

public final class InternCrash {

    public static String[] intern(String[] a) {
        return a;
    }

    public static Object intern(Object a) {
        if (a instanceof String[]) {
            String[] asArray = (String[]) a;
            return intern(asArray);
        } else {
            return null;
        }
    }
}
