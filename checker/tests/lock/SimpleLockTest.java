import org.checkerframework.checker.lock.qual.GuardedBy;

public class SimpleLockTest {
  final Object lock1 = new Object(), lock2 = new Object();

  void testMethodCall(@GuardedBy("lock1") SimpleLockTest this) {
    // :: error: (lock.not.held)
    synchronized (lock1) {
    }
    // :: error: (lock.not.held)
    synchronized (this.lock1) {
    }
    // :: error: (lock.not.held)
    synchronized (lock2) {
    }
    // :: error: (lock.not.held)
    synchronized (this.lock2) {
    }

    final @GuardedBy("myClass.field") MyClass myClass = new MyClass();
    // :: error: (lock.not.held)
    synchronized (myClass.field) {
    }
    synchronized (myClass) {
    }
  }

  class MyClass {
    final Object field = new Object();
  }
}
