import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.*;

public class Overriding {

  class SuperClass {
    protected Object a, b, c;

    @Holding("a")
    void guardedByOne() {}

    @Holding({"a", "b"})
    void guardedByTwo() {}

    @Holding({"a", "b", "c"})
    void guardedByThree() {}

    @ReleasesNoLocks
    void rnlMethod() {
      // :: error: (method.guarantee.violated)
      mrlMethod();
      rnlMethod();
      implicitRnlMethod();
      lfMethod();
    }

    void implicitRnlMethod() {
      // :: error: (method.guarantee.violated)
      mrlMethod();
      rnlMethod();
      implicitRnlMethod();
      lfMethod();
    }

    @LockingFree
    void lfMethod() {
      // :: error: (method.guarantee.violated)
      mrlMethod();
      // :: error: (method.guarantee.violated)
      rnlMethod();
      // :: error: (method.guarantee.violated)
      implicitRnlMethod();
      lfMethod();
    }

    @MayReleaseLocks
    void mrlMethod() {
      mrlMethod();
      rnlMethod();
      implicitRnlMethod();
      lfMethod();
    }

    @ReleasesNoLocks
    void rnlMethod2() {}

    void implicitRnlMethod2() {}

    @LockingFree
    void lfMethod2() {}

    @MayReleaseLocks
    void mrlMethod2() {}

    @ReleasesNoLocks
    void rnlMethod3() {}

    void implicitRnlMethod3() {}

    @LockingFree
    void lfMethod3() {}
  }

  class SubClass extends SuperClass {
    @Holding({"a", "b"}) // error
    @Override
    // :: error: (contracts.precondition.override.invalid)
    void guardedByOne() {}

    @Holding({"a", "b"})
    @Override
    void guardedByTwo() {}

    @Holding({"a", "b"})
    @Override
    void guardedByThree() {}

    @MayReleaseLocks
    @Override
    // :: error: (override.sideeffect.invalid)
    void rnlMethod() {}

    @MayReleaseLocks
    @Override
    // :: error: (override.sideeffect.invalid)
    void implicitRnlMethod() {}

    @ReleasesNoLocks
    @Override
    // :: error: (override.sideeffect.invalid)
    void lfMethod() {}

    @MayReleaseLocks
    @Override
    void mrlMethod() {}

    @ReleasesNoLocks
    @Override
    void rnlMethod2() {}

    @Override
    void implicitRnlMethod2() {}

    @LockingFree
    @Override
    void lfMethod2() {}

    @ReleasesNoLocks
    @Override
    void mrlMethod2() {}

    @LockingFree
    @Override
    void rnlMethod3() {}

    @LockingFree
    @Override
    void implicitRnlMethod3() {}

    @SideEffectFree
    @Override
    void lfMethod3() {}
  }

  // Test overriding @Holding with JCIP @GuardedBy.
  class SubClassJcip extends SuperClass {
    @net.jcip.annotations.GuardedBy({"a", "b"}) @Override
    // :: error: (contracts.precondition.override.invalid)
    void guardedByOne() {}

    @net.jcip.annotations.GuardedBy({"a", "b"}) @Override
    void guardedByTwo() {}

    @net.jcip.annotations.GuardedBy({"a", "b"}) @Override
    void guardedByThree() {}
  }

  // Test overriding @Holding with Javax @GuardedBy.
  class SubClassJavax extends SuperClass {
    @javax.annotation.concurrent.GuardedBy({"a", "b"}) @Override
    // :: error: (contracts.precondition.override.invalid)
    void guardedByOne() {}

    @javax.annotation.concurrent.GuardedBy({"a", "b"}) @Override
    void guardedByTwo() {}

    @javax.annotation.concurrent.GuardedBy({"a", "b"}) @Override
    void guardedByThree() {}
  }
}
