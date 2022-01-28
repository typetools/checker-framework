package parent.child;

import org.jspecify.nullness.Nullable;

public class NotNullMarkedBecauseChildPackage<T> {
    void foo(NotNullMarkedBecauseChildPackage<@Nullable String> d) {}
}
