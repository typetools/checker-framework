// Test that a call to a method that might release locks makes a lock that a method call returns
// @LockPossiblyHeld, even if the store keeps the value of the method call.

import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.dataflow.qual.Pure;

public class MethodCallLockAfterSideEffect {

  static final ReentrantLock lock = new ReentrantLock();

  // The store keeps the value of a call to this method across a call to a method that has side
  // effects, because no code can change the value that this method returns.
  @Pure
  static ReentrantLock getLock() {
    return lock;
  }

  @MayReleaseLocks
  static void releaseLock() {
    lock.unlock();
  }

  @Holding("MethodCallLockAfterSideEffect.getLock()")
  static void requiresLock() {}

  void lockThenCall() {
    getLock().lock();
    requiresLock();
  }

  @MayReleaseLocks
  void lockThenRelease() {
    getLock().lock();
    releaseLock();
    // :: error: [contracts.precondition]
    requiresLock();
  }
}
