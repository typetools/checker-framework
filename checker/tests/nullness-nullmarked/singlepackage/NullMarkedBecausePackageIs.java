package other;

import org.jspecify.nullness.Nullable;

public class NullMarkedBecausePackageIs<T> {
    // :: error: (type.argument.type.incompatible)
    void foo(NullMarkedBecausePackageIs<@Nullable String> d) {}
}
