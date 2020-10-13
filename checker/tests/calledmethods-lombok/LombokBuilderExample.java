// A test for support for the builder() method in Lombok builders.

import lombok.Builder;

@Builder
class LombokBuilderExample {
    @lombok.NonNull Object foo;
    @lombok.NonNull Object bar;

    static void test() {
        // :: error: (finalizer.invocation.invalid)
        builder().build();
    }
}
