import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;

// A call to a method that might release locks (such as a @MayReleaseLocks method) might release any
// lock, including a lock that a method call returns.
public class ImpureCallReleasesLock {

  static final ReentrantLock LOCK = new ReentrantLock();

  @Pure
  static ReentrantLock pureGetLock() {
    return LOCK;
  }

  static int counter = 0;

  @Deterministic
  @MayReleaseLocks
  static ReentrantLock impureGetLock() {
    counter++;
    return LOCK;
  }

  @MayReleaseLocks
  static void sideEffect() {}

  @Holding("ImpureCallReleasesLock.pureGetLock()")
  static void requiresPureGetLock() {}

  @Holding("ImpureCallReleasesLock.impureGetLock()")
  static void requiresImpureGetLock() {}

  // The store retains the value of a deterministic static method call with no arguments across a
  // call to a method that might release locks, but that call might release the lock.
  @MayReleaseLocks
  void retainedMethodCall() {
    pureGetLock().lock();
    requiresPureGetLock();
    sideEffect();
    // :: error: [contracts.precondition]
    requiresPureGetLock();
  }

  // A call to a method that might release locks might release the lock that it returns.
  @MayReleaseLocks
  void callResult() {
    impureGetLock().lock();
    requiresImpureGetLock();
    impureGetLock();
    // :: error: [contracts.precondition]
    requiresImpureGetLock();
  }
}
