// Test Case for Issue2215
// https://github.com/typetools/checker-framework/issues/2215
// @skip-test

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class ParentClass {

    @MonotonicNonNull Object parentClassField;

    ParentClass(Object o) {
        this.parentClassField = o; // should be allowed
    }
}

public class Issue2215 extends ParentClass {

    @MonotonicNonNull Object f;
    @MonotonicNonNull Object g = null;
    @MonotonicNonNull Object h = new Object();

    Issue2215(@Nullable Object o) {

        super(o); // should be allowed
        this.parentClassField = o; // should be allowed
        this.f = o; // should be allowed
        this.f = null; // should be allowed
        this.g = o; // should be allowed
        this.g = null; // should be allowed
        this.h = o; // should not be allowed
        this.h = null; // should not be allowed
    }
}
