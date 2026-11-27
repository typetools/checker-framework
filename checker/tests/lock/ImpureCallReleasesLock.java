import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.EnsuresLockHeld;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.dataflow.qual.Deterministic;

// A call to a method that might release locks might release the lock that the call returns, even
// if the store has a value for the call from before the call.
public abstract class ImpureCallReleasesLock {

  static final ReentrantLock LOCK = new ReentrantLock();

  static int counter = 0;

  @Deterministic
  @MayReleaseLocks
  static ReentrantLock getLockMayRelease() {
    counter++;
    LOCK.unlock();
    return LOCK;
  }

  @Holding("#1")
  static void requiresArg(ReentrantLock l) {}

  @MayReleaseLocks
  void viaLocal() {
    getLockMayRelease().lock();
    ReentrantLock l = getLockMayRelease();
    // :: error: [contracts.precondition]
    requiresArg(l);
  }

  @MayReleaseLocks
  void viaArgument() {
    getLockMayRelease().lock();
    // :: error: [contracts.precondition]
    requiresArg(getLockMayRelease());
  }

  // A postcondition of the call establishes that the lock that the call returns is held.
  @Deterministic
  @MayReleaseLocks
  @EnsuresLockHeld("getLock()")
  abstract ReentrantLock getLock();

  @MayReleaseLocks
  void postconditionViaLocal() {
    getLock();
    ReentrantLock l = getLock();
    requiresArg(l);
  }

  @MayReleaseLocks
  void postconditionViaArgument() {
    getLock();
    requiresArg(getLock());
  }
}
