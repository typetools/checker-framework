// Test case for https://github.com/typetools/checker-framework/issues/5200

import org.checkerframework.checker.nullness.qual.Nullable;

class Test {
    record Foo(@Nullable String bar) {
        @Nullable String baz() {
            return Math.random() > 0.5 ? bar : null;
        }
    }

    void main() {
        checkEmpty(new Foo(""));
    }

    void checkEmpty(Foo foo) {
        if (foo.bar() != null && !foo.bar().isEmpty()) {
            System.out.println("ok");
        }
        // :: error: (dereference.of.nullable)
        if (foo.baz() != null && !foo.baz().isEmpty()) {
            System.out.println("not ok");
        }
    }
}
