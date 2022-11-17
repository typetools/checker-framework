package parent.child;

import org.jspecify.annotations.Nullable;

public class NotNullMarkedBecauseChildPackage<T> {
    void foo(NotNullMarkedBecauseChildPackage<@Nullable String> d) {}
}
