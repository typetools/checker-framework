// Test case for Issue 2229:
// https://github.com/typetools/checker-framework/issues/2229

import org.checkerframework.checker.lock.qual.*;

@GuardedBy("lock") class DependentTypesNPE {
    // :: error: (expression.unparsable.type.invalid)
    @GuardedBy("lock") DependentTypesNPE() {}
}
