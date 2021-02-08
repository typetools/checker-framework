import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.List;

class DefaultForEach {
    @DefaultQualifier(Nullable.class)
    Object @NonNull [] foo() {
        return new Object[] {null};
    }

    void bar() {
        for (Object p : foo()) {
            // :: error: dereference.of.nullable
            p.toString();
        }
    }

    @DefaultQualifier(Nullable.class)
    @NonNull List<Object> foo2() {
        throw new RuntimeException();
    }

    void bar2() {
        for (Object p : foo2()) {
            // :: error: (dereference.of.nullable)
            p.toString();
        }
    }
}
