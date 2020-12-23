// Test case for Issue 403:
// https://github.com/typetools/checker-framework/issues/403

import java.util.Comparator;

public class Issue403 {
    Comparator<Issue403> COMPARATOR = Comparator.comparing(w -> w.value);

    String value;

    Issue403(final String value) {
        this.value = value;
    }
}
