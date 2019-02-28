import org.checkerframework.checker.lock.qual.*;

class Issue2163 {
    @GuardedBy Issue2163() {}

    void test() {
        // :: error: (constructor.invocation.invalid) :: error: (guardsatisfied.location.disallowed)
        new @GuardSatisfied Issue2163();
    }
}
