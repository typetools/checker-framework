import org.checkerframework.checker.lock.qual.*;

@GuardedBy("lock") class Issue2186NPE {
    @GuardedBy("lock") Issue2186NPE() {}
}
