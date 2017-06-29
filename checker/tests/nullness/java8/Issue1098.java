// Test case for Issue 1098:
// https://github.com/typetools/checker-framework/issues/1098

// @below-java8-jdk-skip-test

// @skip-test until the issue is fixed

import java.util.Optional;

class Issue1098 {
    <T> void opt(Optional<T> p1, T p2) {}

    <T> void cls(Class<T> p1, T p2) {}

    void use() {
        opt(Optional.empty(), null);
        cls(this.getClass(), null);
    }
}
