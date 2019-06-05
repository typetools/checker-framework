// This is a test case for issue 2247:
// https://github.com/typetools/checker-framework/issues/2247

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue2247 {}

@NonNull class DeclaredClass {}

class ValidUseType {
    void test() {
        // :: error: (type.invalid.annotations.on.use)
        @Nullable DeclaredClass object;
    }
}
