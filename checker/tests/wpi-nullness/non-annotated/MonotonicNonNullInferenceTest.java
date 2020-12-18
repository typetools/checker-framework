// :: error: (initialization.static.fields.uninitialized)
public class MonotonicNonNullInferenceTest {

    static String staticString1;

    // :: error: (assignment.type.incompatible)
    static String staticString2 = null;

    static String staticString3;

    String instanceString1;

    // :: error: (assignment.type.incompatible)
    String instanceString2 = null;

    String instanceString3;

    static {
        // :: error: (assignment.type.incompatible)
        staticString3 = null;
    }

    // :: error: (initialization.fields.uninitialized)
    MonotonicNonNullInferenceTest() {
        String instanceString3 = "hello";
    }

    static void m1(String arg) {
        staticString1 = arg;
        staticString2 = arg;
        staticString3 = arg;
    }

    void m2(String arg) {
        instanceString1 = arg;
        instanceString2 = arg;
        instanceString3 = arg;
    }
}
