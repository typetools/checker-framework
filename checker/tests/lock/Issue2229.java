import org.checkerframework.checker.lock.qual.*;

@GuardedBy("lock") class Issue2229 {
    // :: error: (expression.unparsable.type.invalid)
    @GuardedBy("lock") Issue2229() {}
}
