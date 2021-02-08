import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

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
}
