import org.checkerframework.checker.nullness.qual.*;

public class AnonymousClass {

    class Bound<X extends @NonNull Object> {}

    void test() {
        // :: error: (type.argument.type.incompatible)
        new Bound<@Nullable String>() {};
    }

    // The dummy parameter tests ParamApplier
    void test(Object dummy) {
        // :: error: (type.argument.type.incompatible)
        new Bound<@Nullable String>() {};
    }
}
