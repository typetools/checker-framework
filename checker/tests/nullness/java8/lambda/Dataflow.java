
// Test dataflow refinement in the body of a lambda.

import tests.util.function.*;
import org.checkerframework.checker.nullness.qual.*;

class Dataflow {
    void context() {
        Function<@Nullable Object, Object> o = a -> {
            //:: error: (dereference.of.nullable)
            a.toString();
            a = "";
            a.toString();
            return "";
        };
    }
}
