// Test case for Issue 559:
// https://github.com/typetools/checker-framework/issues/559

import java.util.Optional;

public class Issue559 {
    void bar(Optional<String> o) {
        // With myjdk.astub the following should fail with an
        // argument.type.incompatible error.
        o.orElse(null);
        o.orElse("Hi");
    }
}
