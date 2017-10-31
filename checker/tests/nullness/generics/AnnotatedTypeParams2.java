import org.checkerframework.checker.nullness.qual.*;

class SomeClass<@Nullable T> {
    T get() {
        throw new RuntimeException();
    }
}

class AnnotatedTypeParams {

    void testPositive() {
        SomeClass<@Nullable String> l = new SomeClass<@Nullable String>();
        // :: error: (dereference.of.nullable)
        l.get().toString();
    }

    void testInvalidParam() {
        // :: error: (type.argument.type.incompatible)
        SomeClass<@NonNull String> l;
    }
}
