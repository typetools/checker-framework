// Test case for Issue #152
// https://github.com/typetools/checker-framework/issues/152

// @skip-test

import org.checkerframework.checker.lock.qual.GuardedBy;

public class ThisSuper {

  class MyClass {
    Object field;
  }

  class LockExample {
    protected final Object myLock = new Object();

    protected @GuardedBy("myLock") MyClass locked;

    @GuardedBy("this.myLock") MyClass m1;

    protected @GuardedBy("this.myLock") MyClass locked2;

    public void accessLock() {
      synchronized (myLock) {
        this.locked.field = new Object();
      }
    }
  }

  class LockExampleSubclass extends LockExample {
    private final Object myLock = new Object();

    private @GuardedBy("this.myLock") MyClass locked;

    @GuardedBy("this.myLock") MyClass m2;

    public LockExampleSubclass() {
      final LockExampleSubclass les1 = new LockExampleSubclass();
      final LockExampleSubclass les2 = new LockExampleSubclass();
      final LockExampleSubclass les3 = les2;
      LockExample le1 = new LockExample();

      synchronized (super.myLock) {
        super.locked.toString();
        super.locked2.toString();
        // :: error: (contracts.precondition.not.satisfied)
        locked.toString();
      }
      synchronized (myLock) {
        // :: error: (contracts.precondition.not.satisfied)
        super.locked.toString();
        // :: error: (contracts.precondition.not.satisfied)
        super.locked2.toString();
        locked.toString();
      }

      // :: error: (assignment.type.incompatible)
      les1.locked = le1.locked;
      // :: error: (assignment.type.incompatible)
      les1.locked = le1.locked2;

      // :: error: (assignment.type.incompatible)
      les1.locked = les2.locked;

      // :: error: (assignment.type.incompatible)
      this.locked = super.locked;
      // :: error: (assignment.type.incompatible)
      this.locked = super.locked2;

      // :: error: (assignment.type.incompatible)
      m1 = m2;
    }

    @Override
    public void accessLock() {
      synchronized (myLock) {
        this.locked.field = new Object();
        // :: error: (lock.not.held)
        super.locked.field = new Object();
        System.out.println(
            this.locked.field
                + " "
                +
                // :: error: (lock.not.held)
                super.locked.field);
        System.out.println("Are locks equal? " + (super.locked == this.locked ? "yes" : "no"));
      }
    }
  }
}
