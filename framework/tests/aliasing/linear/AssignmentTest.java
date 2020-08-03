import org.checkerframework.common.aliasing.qual.Linear;

public class AssignmentTest {

    void Test() {
        @Linear String s = getLinear();

        // :: error: (linear.leaked)
        String b = s;
        // Just like unique objects, linear objects cannot share a reference

        boolean c = s.isEmpty();
        // Due to the method invocation, s in now unusable

        // :: error: (use.unsafe)
        String d = s.toUpperCase();
    }

    @SuppressWarnings("return.type.incompatible")
    static @Linear String getLinear() {
        return "hi";
    }
}
