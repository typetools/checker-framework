package other;

import org.jspecify.annotations.Nullable;

public class NullMarkedBecausePackageIs<T> {
    // :: error: (type.argument.type.incompatible)
    void foo(NullMarkedBecausePackageIs<@Nullable String> d) {}
}
