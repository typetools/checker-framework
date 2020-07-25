import org.checkerframework.checker.linear.qual.Linear;

public class LinearTest {

    void Test() {
        @Linear String s = getLinearString();

        String b = s.toLowerCase();
        // Due to the method invocation, s is used up and is now unusable
        // Since s is unusable now, it can't be used for assignment, method invocation or as an
        // argument

        // :: error: (use.unsafe)
        boolean c = s.isEmpty();

        // :: error: (use.unsafe)
        String d = s.toUpperCase();
    }

    static @Linear String getLinearString() {
        return "Linear string";
    }
}
