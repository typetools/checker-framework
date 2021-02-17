// This class is not compiled with the Nullness Checker,
// so that only explicit annotations are stored in bytecode.

import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Binary {
    @Nullable Object foo() {
        return null;
    }

    Object bar(Object p) {
        return null;
    }

    int baz(Object @NonNull [] p) {
        return 1;
    }

    int baz2(Object[] p) {
        return 1;
    }
}
