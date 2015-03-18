// Test case for Issue 403:
// https://code.google.com/p/checker-framework/issues/detail?id=403

import java.util.Comparator;

class Issue403 {
    Comparator<Issue403> COMPARATOR = Comparator.comparing(w -> w.value);

    String value;

    Issue403(final String value) {
        this.value = value;
    }
}
