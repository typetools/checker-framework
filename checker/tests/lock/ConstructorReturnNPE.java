// Test case for Issue 2229:
// https://github.com/typetools/checker-framework/issues/2229

import org.checkerframework.checker.lock.qual.*;

// :: error: (expression.unparsable.type.invalid)
@GuardedBy("lock") class ConstructorReturnNPE {
    // :: error: (expression.unparsable.type.invalid) :: error: (super.invocation.invalid)
    // :: warning: (inconsistent.constructor.type)
    @GuardedBy("lock") ConstructorReturnNPE() {}
}
