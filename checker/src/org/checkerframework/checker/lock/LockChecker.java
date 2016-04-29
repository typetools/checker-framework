package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;

// TODO: The Lock Checker implementation has not been optimized for performance yet.
// Run a profiler on the Lock Checker tests, as well as when type checking a large project such as Daikon or Guava.

// TODO: Here is a list of nice-to-have features/tests but not critical to release the Lock Checker:

// Add a warning if a user annotates a static field with @GuardedBy("this") instead of @GuardedBy("<class name>.class")

// Add a test that @GuardedBy("<class name>.class") is never ambiguous given two classes with the same name in two different packages.

// In LockStore.updateForMethodCall, calling a method annotated with @MayReleaseLocks should not
// always cause local variables' refinement to be reset to @GuardedByUnknown.
// The current workaround is to explicitly annotate the local variable with the appropriate annotation in the @GuardedBy hierarchy.

// Would it be a useful feature for the Lock Checker to warn about missing unlock calls when there is a call to .lock() in a method?
// Or is this a common pattern to lock in one method and unlock in a different one?

// Issue an error if @GuardSatisfied is written on a location other than a primary annotation.

/**
 * @checker_framework.manual #lock-checker Lock Checker
 */
public class LockChecker extends BaseTypeChecker {
}
