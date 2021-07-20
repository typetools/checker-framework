// @skip-test

// Test case for Issue #917:
// https://github.com/typetools/checker-framework/issues/917

import org.checkerframework.checker.lock.qual.GuardSatisfied;

import java.util.List;

public class GuardSatisfiedArray {

    void foo(@GuardSatisfied Object arg1, @GuardSatisfied Object arg2) {}

    void bar(@GuardSatisfied Object[] args) {
        foo(args[0], args[1]);
    }

    void baz(@GuardSatisfied List<@GuardSatisfied Object> args) {
        foo(args.get(0), args.get(1));
    }
}
