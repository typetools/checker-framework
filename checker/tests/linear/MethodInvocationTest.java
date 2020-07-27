import org.checkerframework.checker.linear.qual.Linear;

public class MethodInvocationTest {

    void Test() {
        @Linear String s = "Linear string";

        // :: error: (normal.parameter.error)
        check(s);
        // Since s is linear, it cannot be passed as a normal parameter as it can be used more than
        // once in the check method.

        checkLinear(s); // No error
    }

    static void check(String s) {}

    static void checkLinear(@Linear String s) {}
}
