// Test case for Issue 403:
// https://github.com/typetools/checker-framework/issues/403

// @below-java8-jdk-skip-test

import java.util.Comparator;

class Issue403 {
    Comparator<Issue403> COMPARATOR = Comparator.comparing(w -> w.value);

    String value;

    Issue403(final String value) {
        this.value = value;
    }
}
