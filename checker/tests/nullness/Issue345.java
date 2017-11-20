// This is a test case for Issue 345:
// https://github.com/typetools/checker-framework/issues/345
public class Issue345 {
    String f1;
    String f2;

    {
        // :: error: (assignment.type.incompatible)
        f1 = f2;
        f2 = f1;
        f2.toString(); // Null pointer exception here
    }

    public static void main(String[] args) {
        Issue345 a = new Issue345();
    }
}
