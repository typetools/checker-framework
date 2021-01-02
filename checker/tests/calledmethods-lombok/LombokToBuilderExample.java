// A test for support for the toBuilder() method in Lombok builders.

import lombok.Builder;

@Builder(toBuilder = true)
public class LombokToBuilderExample {
    @lombok.NonNull String req;

    static void test(LombokToBuilderExample foo) {
        foo.toBuilder().build();
    }

    static void ensureThatErrorIssued() {
        // :: error: finalizer.invocation.invalid
        LombokToBuilderExample.builder().build();
    }
}
