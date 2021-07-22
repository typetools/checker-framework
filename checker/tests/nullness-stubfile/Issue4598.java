// Test case for Issue #4598

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class Issue4598 {

    final @Nullable Object d = null;

    public Object foo() {
        Objects.requireNonNull(d, "destination");
        // :: error: (return)
        return d;
    }
}
