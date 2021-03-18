package org.checkerframework.dataflow.util;

import java.util.Collections;
import java.util.IdentityHashMap;
import org.checkerframework.checker.interning.qual.FindDistinct;
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
    public boolean add(@FindDistinct T e) {
        switch (state) {
            case EMPTY:
                state = State.SINGLETON;
                value = e;
                return true;
            case SINGLETON:
                if (value == e) {
                    return false;
                }
                makeNonSingleton();
                // fall through
            case ANY:
                assert set != null : "@AssumeAssertion(nullness): set initialized before";
                return set.add(e);
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }

    /** Switch the representation of this from SINGLETON to ANY. */
    private void makeNonSingleton() {
        state = State.ANY;
        set = Collections.newSetFromMap(new IdentityHashMap<>());
        assert value != null : "@AssumeAssertion(nullness): previous add is non-null";
        set.add(value);
        value = null;
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
                return set.contains(o);
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }
}
