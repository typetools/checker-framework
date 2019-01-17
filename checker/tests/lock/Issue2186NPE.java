// @skip-test until https://github.com/t-rasmud/checker-framework/issues/73

import org.checkerframework.checker.lock.qual.*;

@GuardedBy("lock") class Issue2186NPE {
    @GuardedBy("lock") Issue2186NPE() {}
}
