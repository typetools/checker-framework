// This class is not compiled with the Nullness Checker,
// so that only explicit annotations are stored in bytecode.

import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;

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
