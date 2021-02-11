// Test case for issue #4248: https://github.com/typetools/checker-framework/issues/4248

import java.util.List;
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

    double[][] foo3() {
        throw new RuntimeException();
    }

    void bar3() {
        for (double[] pa : foo3()) {
            for (Double p : pa) {
                p.toString();
            }
        }
    }
}
