import org.checkerframework.checker.linear.qual.Unusable;

public class UnusableTest {

    // Method returns unusable string
    @SuppressWarnings("return.type.incompatible")
    static @Unusable String getUnusableString() {
        return "Unusable string";
    }

    void Test() {
        @Unusable String s = getUnusableString();
        // Since s is unusable now, it can't be used for assignment, method invocation or as an
        // argument

        // :: error: (use.unsafe)
        String a = s;

        // :: error: (use.unsafe)
        String b = s.toUpperCase();

        // :: error: (use.unsafe)
        check(s);
    }

    // Method to check whether string s can be passed as an argument
    void check(String s) {}
}
