import org.checkerframework.checker.linear.qual.Linear;

public class AssignmentTest {

    void Test() {
        @Linear String s = "Linear string";

        String b = s;
        // Due to the assignment, s is used up and is now unusable whereas b is now linear
        // Since s is unusable now, it can't be used for assignment, method invocation or as an
        // argument

        // :: error: (use.unsafe)
        boolean c = s.isEmpty();

        // :: error: (use.unsafe)
        String d = s.toUpperCase();

        String e = b.toLowerCase();
        // Due to the method invocation, b is used up and is now unusable
        // Since b is unusable now, it can't be used for assignment, method invocation or as an
        // argument

        // :: error: (use.unsafe)
        String f = b.toUpperCase();
    }
}
