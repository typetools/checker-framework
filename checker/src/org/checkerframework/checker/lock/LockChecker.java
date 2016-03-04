package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;

//TODO: Here is a list of nice-to-have features/tests but not critical to release the Lock Checker:

//Add an enhancement so that implicit .toString() calls are de-sugared, e.g. "foo" + myVariable + "bar"
//is handled as "foo" + myVariable.toString() + "bar". This is important so that LockVisitor handles
//myVariable as the receiver parameter of a method call.

//Add a warning if a user annotates a static field with @GuardedBy("this") instead of @GuardedBy("<class name>.class")

//Add a test that @GuardedBy("<class name>.class") is never ambiguous given two classes with the same name in two different packages.

//Calling a method annotated with @MayReleaseLocks should not always cause local variables' refinement to be reset to @GuardedByUnknown.
//The current workaround is to explicitly annotate the local variable with the appropriate annotation in the @GuardedBy hierarchy.

//Would it be a useful feature for the Lock Checker to warn about missing unlock calls when there is a call to .lock() in a method?
//Or is this a common pattern to lock in one method and unlock in a different one?

//Issue an error if @GuardSatisfied is written on a location other than a primary annotation.

//TODO: The following bugs are important to fix before the March 1 release of the Checker Framework:
//TODO: Issue a warning if a lock is not final or effectively final. Document in the manual.
//    Nice to have: consider whether @Pure methods make this warning unnecessary in some scenarios.
//TODO: Make sure "itself" is not handled by the flow expression parser for type hierarchies other than @GuardedBy.

/**
 * @checker_framework.manual #lock-checker Lock Checker
 */
public class LockChecker extends BaseTypeChecker {
}
