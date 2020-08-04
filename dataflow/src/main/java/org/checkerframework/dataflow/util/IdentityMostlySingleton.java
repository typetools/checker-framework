package org.checkerframework.dataflow.util;

import java.util.ArrayList;
import org.checkerframework.javacutil.BugInCF;

/**
 * An arbitrary-size set that is very efficient (more efficient than HashSet) for 0 and 1 elements.
 * Uses object identity for object comparison.
 */
public final class IdentityMostlySingleton<T extends Object> extends AbstractMostlySingleton<T> {

    /** Create an IdentityMostlySingleton. */
    public IdentityMostlySingleton() {
        super(State.EMPTY);
    }

    /** Create an IdentityMostlySingleton. */
    public IdentityMostlySingleton(T value) {
        super(State.SINGLETON, value);
    }

    @Override
    @SuppressWarnings("fallthrough")
    public boolean add(T e) {
        switch (state) {
            case EMPTY:
                state = State.SINGLETON;
                value = e;
                return true;
            case SINGLETON:
                if (value == e) {
                    return false;
                }
                state = State.ANY;
                // Use ArrayList, but make sure to use reference comparisons.
                set = new ArrayList<>();
                assert value != null : "@AssumeAssertion(nullness): previous add is non-null";
                set.add(value);
                value = null;
                // fallthrough
            case ANY:
                assert set != null : "@AssumeAssertion(nullness): set initialized before";
                for (T x : set) {
                    if (x == e) {
                        return false;
                    }
                }
                return set.add(e);
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }

    @SuppressWarnings("interning:not.interned") // this class uses object identity
    @Override
    public boolean contains(Object o) {
        switch (state) {
            case EMPTY:
                return false;
            case SINGLETON:
                return o == value;
            case ANY:
                assert set != null : "@AssumeAssertion(nullness): set initialized before";
                for (T x : set) {
                    if (x == o) {
                        return true;
                    }
                }
                return false;
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }
}
