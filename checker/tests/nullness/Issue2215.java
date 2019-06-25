// Test Case for Issue2215
// https://github.com/typetools/checker-framework/issues/2215
// @skip-test until the bug is fixed

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class ParentClass {

    @MonotonicNonNull Object parentClassField;

    ParentClass(@Nullable Object o) {
        this.parentClassField = o;
    }
}

public class Issue2215 extends ParentClass {

    @MonotonicNonNull Object f;
    @MonotonicNonNull Object g = null;
    @MonotonicNonNull Object h = new Object();

    Issue2215(@Nullable Object o) {

        super(o);
        this.parentClassField = o;
        this.f = o;
        this.f = null;
        this.g = o;
        this.g = null;
        // :: error: (assignment.type.incompatible)
        this.h = o;
        // :: error: (assignment.type.incompatible)
        this.h = null;
    }
}
