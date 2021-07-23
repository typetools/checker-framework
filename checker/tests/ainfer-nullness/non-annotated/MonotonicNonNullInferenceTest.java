import org.checkerframework.checker.nullness.qual.NonNull;

public class MonotonicNonNullInferenceTest {

    // :: warning: (initialization.static.field.uninitialized)
    static String staticString1;

    // :: warning: (assignment.type.incompatible)
    static String staticString2 = null;

    static String staticString3;

    String instanceString1;

    // :: warning: (assignment.type.incompatible)
    String instanceString2 = null;

    String instanceString3;

    static {
        // :: warning: (assignment.type.incompatible)
        staticString3 = null;
    }

    // :: warning: (initialization.fields.uninitialized)
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

    void hasSideEffect() {}

    void testMonotonicNonNull() {
        @NonNull String s;
        if (staticString1 != null) {
            hasSideEffect();
            s = staticString1;
        }
        if (staticString2 != null) {
            hasSideEffect();
            s = staticString2;
        }
        if (staticString3 != null) {
            hasSideEffect();
            s = staticString3;
        }
        if (instanceString1 != null) {
            hasSideEffect();
            s = instanceString1;
        }
        if (instanceString2 != null) {
            hasSideEffect();
            s = instanceString2;
        }
        if (instanceString3 != null) {
            hasSideEffect();
            s = instanceString3;
        }
    }
}
