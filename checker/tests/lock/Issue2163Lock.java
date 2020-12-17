import org.checkerframework.checker.lock.qual.*;

public class Issue2163Lock {
    @GuardedBy Issue2163Lock() {}

    void test() {
        // :: error: (constructor.invocation.invalid) :: error: (guardsatisfied.location.disallowed)
        new @GuardSatisfied Issue2163Lock();
    }
}
