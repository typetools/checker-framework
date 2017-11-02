import org.checkerframework.checker.nullness.compatqual.*;
import org.checkerframework.checker.nullness.qual.*;

class Java7Aliases {
    @NullableDecl Object f;
    @Nullable Object g;

    void foo() {
        // :: error: (dereference.of.nullable)
        f.toString();
        // :: error: (dereference.of.nullable)
        g.toString();
    }
}
