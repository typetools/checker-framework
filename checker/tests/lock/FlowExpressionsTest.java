import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public class FlowExpressionsTest {
  class MyClass {
    public Object field;
  }

  private final @GuardedBy({"<self>"}) MyClass m = new MyClass();
  // private @GuardedBy({"nonexistentfield"}) MyClass m2;
  @Pure
  private @GuardedBy({"<self>"}) MyClass getm() {
    return m;
  }

  public void method() {
    // :: error: (lock.not.held)
    getm().field = new Object();
    // :: error: (lock.not.held)
    m.field = new Object();
    // TODO: fix the Lock Checker code so that a flowexpr.parse.error is issued (due to the
    // guard of "nonexistentfield" on m2)
    // m2.field = new Object();
    synchronized (m) {
      m.field = new Object();
    }
    synchronized (getm()) {
      getm().field = new Object();
    }
  }
}
