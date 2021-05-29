// Test case for Issue #770
// https://github.com/typetools/checker-framework/issues/770

import org.checkerframework.checker.lock.qual.GuardedBy;

public class ViewpointAdaptation2 {

  class LockExample {
    protected final Object myLock = new Object();

    protected @GuardedBy("myLock") Object locked;

    protected @GuardedBy("this.myLock") Object locked2;

    public @GuardedBy("myLock") Object getLocked() {
      return locked;
    }
  }

  class Use {
    final LockExample lockExample1 = new LockExample();
    final Object myLock = new Object();

    @GuardedBy("lockExample1.myLock") Object o1 = lockExample1.locked;

    @GuardedBy("lockExample1.myLock") Object o2 = lockExample1.locked2;
    // :: error: (assignment)
    @GuardedBy("myLock") Object o3 = lockExample1.locked;
    // :: error: (assignment)
    @GuardedBy("this.myLock") Object o4 = lockExample1.locked2;

    @GuardedBy("lockExample1.myLock") Object oM1 = lockExample1.getLocked();
    // :: error: (assignment)
    @GuardedBy("myLock") Object oM2 = lockExample1.getLocked();
    // :: error: (assignment)
    @GuardedBy("this.myLock") Object oM3 = lockExample1.getLocked();

    void uses() {
      lockExample1.locked = o1;
      // :: error: (assignment)
      lockExample1.locked = o3;
    }
  }
}
