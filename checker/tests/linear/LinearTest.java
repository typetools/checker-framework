import org.checkerframework.checker.linear.qual.Linear;

public class LinearTest {

    void Test() {
        @Linear String s = "hi";
        String a = s; // Assignment doesn't use up variables, hence, s is still linear
        check(s); // Passing s as an argument doesn't use it up, hence, s is still linear

        String b =
                s.toLowerCase(); // Due to the method invocation, s is used up and is now unusable
        // Since s is unusable now, it can't be used for assignment, method invocation or as an
        // argument

        // :: error: (use.unsafe)
        String c = s;

        // :: error: (use.unsafe)
        String d = s.toUpperCase();

        // :: error: (use.unsafe)
        check(s);
    }

    // Method to check whether string s can be passed as an argument
    void check(String s) {}
}
