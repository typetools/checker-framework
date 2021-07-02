import java.util.concurrent.locks.*;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.framework.qual.AnnotatedFor;

public class BasicLockTest {
  class MyClass {
    public Object field;
  }

  Object someValue = new Object();

  MyClass newMyClass = new MyClass();

  MyClass myUnannotatedMethod(MyClass param) {
    return param;
  }

  void myUnannotatedMethod2() {}

  @AnnotatedFor("lock")
  MyClass myAnnotatedMethod(MyClass param) {
    return param;
  }

  @AnnotatedFor("lock")
  void myAnnotatedMethod2() {}

  final @GuardedBy({}) ReentrantLock lockField = new ReentrantLock();

  @GuardedBy("lockField") MyClass m;

  @GuardedBy({}) MyClass o1 = new MyClass(), p1;

  @AnnotatedFor("lock")
  @MayReleaseLocks
  void testFields() {
    // Test in two ways that return values are @GuardedByUnknown.
    // The first way is more durable as cannot.dereference is tied specifically to
    // @GuardedByUnknown (and @GuardedByBottom, but it is unlikely to become the default for
    // return values on unannotated methods).
    // :: error: (lock.not.held) :: error: (argument)
    myUnannotatedMethod(o1).field = someValue;
    // The second way is less durable because the default for fields is currently @GuardedBy({})
    // but could be changed to @GuardedByUnknown.
    // :: error: (assignment) :: error: (argument)
    p1 = myUnannotatedMethod(o1);

    // Now test that an unannotated method behaves as if it's annotated with @MayReleaseLocks
    lockField.lock();
    myAnnotatedMethod2();
    m.field = someValue;
    myUnannotatedMethod2();
    // :: error: (lock.not.held)
    m.field = someValue;
  }

  void unannotatedReleaseLock(ReentrantLock lock) {
    lock.unlock();
  }

  @AnnotatedFor("lock")
  @MayReleaseLocks
  void testLocalVariables1() {
    MyClass o2 = new MyClass(), p2;
    // :: error: (argument)
    p2 = myUnannotatedMethod(o2);
    MyClass o3 = new MyClass();
    myAnnotatedMethod(o3);
  }

  @AnnotatedFor("lock")
  @MayReleaseLocks
  void testLocalVariables2() {
    // Now test that an unannotated method behaves as if it's annotated with @MayReleaseLocks
    final @GuardedBy({}) ReentrantLock lock = new ReentrantLock();
    @SuppressWarnings("lock:assignment") // prevents flow-sensitive type refinement
    @GuardedBy("lock") MyClass q = newMyClass;
    lock.lock();
    myAnnotatedMethod2();
    q.field = someValue;
    // Should behave as @MayReleaseLocks, and *should* reset @LockHeld assumption about local
    // variable lock.
    myUnannotatedMethod2();
    // :: error: (lock.not.held)
    q.field = someValue;
  }

  @AnnotatedFor("lock")
  @MayReleaseLocks
  void testLocalVariables3() {
    // Now test that an unannotated method behaves as if it's annotated with @MayReleaseLocks
    final @GuardedBy({}) ReentrantLock lock = new ReentrantLock();
    @SuppressWarnings("lock:assignment") // prevents flow-sensitive type refinement
    @GuardedBy("lock") MyClass q = newMyClass;
    lock.lock();
    // Should behave as @MayReleaseLocks, and *should* reset @LockHeld assumption about local
    // variable lock.
    // :: error: (argument)
    unannotatedReleaseLock(lock);
    // :: error: (lock.not.held)
    q.field = someValue;
  }
}
