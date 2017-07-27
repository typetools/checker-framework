// Test case for Issue 1098:
// https://github.com/typetools/checker-framework/issues/1098

// @below-java8-jdk-skip-test

@SuppressWarnings({"nullness", "initialization.fields.uninitialized"})
class MyObject {
    Class<?> getMyClass() {
        return null;
    }
}

class Issue1098NoJdk {
    <T> void cls2(Class<T> p1, T p2) {}

    void use2(MyObject ths) {
        // TODO: false positive, because type agrument inference does not account for @Covariant.
        // See https://github.com/typetools/checker-framework/issues/979.
        //:: error: (argument.type.incompatible)
        cls2(ths.getMyClass(), null);
    }
}
