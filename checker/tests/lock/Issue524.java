// Test case for Issue 524:
// https://github.com/typetools/checker-framework/issues/524

import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.GuardedBy;

// WARNING: this test is nondeterministic, and has already been
// minimized - if you modify it by removing what appears to be
// redundant code, it may no longer reproduce the issue or provide
// coverage for the issue after a fix for the issue has been made.

// About the nondeterminism:
// The desired behavior, with the fix for issue 524 in place, is for the test to type check without
// issuing any warnings.
// (Notice that there are no expected warnings below.)
// However even without a fix for issue 524 in place, the test sometimes type checks.
// Unfortunately a test case that always fails to typecheck using a Checker Framework build
// prior to the fix for issue 524 has not been found.
public class Issue524 {
  class MyClass {
    public Object field;
  }

  void testLocalVariables() {
    @GuardedBy({}) ReentrantLock localLock = new ReentrantLock();

    {
      // :: error: (lock.expression.not.final)
      @GuardedBy("localLock") MyClass q = new MyClass();
      localLock.lock();
      localLock.lock();
      // Without a fix for issue 524 in place, the error lock.not.held
      // (unguarded access to field, variable or parameter 'q' guarded by 'localLock') is
      // issued for the following line.
      // :: error: (expression.unparsable.type.invalid)
      q.field.toString();
    }
  }
}
