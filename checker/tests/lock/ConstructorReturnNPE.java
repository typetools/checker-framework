// Test case for Issue 2229:
// https://github.com/typetools/checker-framework/issues/2229

import org.checkerframework.checker.lock.qual.*;

// :: error: (expression.unparsable)
@GuardedBy("lock") class ConstructorReturnNPE {
  // :: error: (expression.unparsable) :: error: (super.invocation)
  @GuardedBy("lock") ConstructorReturnNPE() {}
}
