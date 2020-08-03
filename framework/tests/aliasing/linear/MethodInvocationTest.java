import org.checkerframework.common.aliasing.qual.Linear;

public class MethodInvocationTest {

    void Test() {
        @Linear String s = getLinear();

        // :: error: (linear.leaked)
        check(s);
        // Just like @Unique, linear objects cannot be passed in method invocations
    }

    static void check(@Linear String s) {}

    @SuppressWarnings("return.type.incompatible")
    static @Linear String getLinear() {
        return "hi";
    }
}
