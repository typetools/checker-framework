import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.LockingFree;

public record LockRecord(String s, ReentrantLock lock) {
  @LockingFree
  // :: error: (method.guarantee.violated)
  public LockRecord {
    // :: error: (method.guarantee.violated)
    lock.lock();
  }
}
