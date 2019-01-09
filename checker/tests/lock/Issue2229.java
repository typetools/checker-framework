import org.checkerframework.checker.lock.qual.*;

@GuardedBy("lock") class Issue2229 {
    @GuardedBy("lock") Issue2229() {}
}
