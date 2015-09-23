import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.*;

public class Overriding {

    class SuperClass {
        protected Object a, b, c;

        @Holding("a")
        void guardedByOne() { }

        @Holding({"a", "b"})
        void guardedByTwo() { }

        @Holding({"a", "b", "c"})
        void guardedByThree() { }

        @ReleasesNoLocks
        void rnlMethod(){ }

        void implicitRnlMethod(){ }

        @LockingFree
        void lfMethod(){ }

        @MayReleaseLocks
        void mrlMethod(){ }

        @ReleasesNoLocks
        void rnlMethod2(){ }

        void implicitRnlMethod2(){ }

        @LockingFree
        void lfMethod2(){ }

        @MayReleaseLocks
        void mrlMethod2(){ }

        @ReleasesNoLocks
        void rnlMethod3(){ }

        void implicitRnlMethod3(){ }

        @LockingFree
        void lfMethod3(){ }
    }

    class SubClass extends SuperClass {
        @Holding({"a", "b"})  // error
        //:: error: (override.holding.invalid)
        @Override void guardedByOne() { }

        @Holding({"a", "b"})
        @Override void guardedByTwo() { }

        @Holding({"a", "b"})
        @Override void guardedByThree() { }

        @MayReleaseLocks
        //:: error: (override.sideeffect.invalid)
        @Override void rnlMethod(){ }

        @MayReleaseLocks
        //:: error: (override.sideeffect.invalid)
        @Override void implicitRnlMethod(){ }

        @ReleasesNoLocks
        //:: error: (override.sideeffect.invalid)
        @Override void lfMethod(){ }

        @MayReleaseLocks
        @Override void mrlMethod(){ }

        @ReleasesNoLocks
        @Override void rnlMethod2(){ }

        @Override void implicitRnlMethod2(){ }

        @LockingFree
        @Override void lfMethod2(){ }

        @ReleasesNoLocks
        @Override void mrlMethod2(){ }

        @LockingFree
        @Override void rnlMethod3(){ }

        @LockingFree
        @Override void implicitRnlMethod3(){ }

        @SideEffectFree
        @Override void lfMethod3(){ }
    }
}
