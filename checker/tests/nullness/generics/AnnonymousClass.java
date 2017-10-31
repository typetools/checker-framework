import org.checkerframework.checker.nullness.qual.*;

class AnonymousClass {

    class Bound<X extends @NonNull Object> {}

    void test() {
        // :: error: (type.argument.type.incompatible)
        new Bound<@Nullable String>() {};
    }
}
