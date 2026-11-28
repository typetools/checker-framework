import org.checkerframework.checker.lock.qual.GuardedBy;

// Unboxing a guarded value yields a primitive copy, which no lock guards.
public class UnboxedCopyNotGuarded {
  final Object lock = new Object();

  @GuardedBy("lock") Object guarded = 0;

  void useCopyOutsideSynchronized() {
    int x;
    synchronized (lock) {
      x = (Integer) guarded;
    }
    System.out.println(x);
  }
}
