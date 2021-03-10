package org.checkerframework.dataflow.util;

import java.util.LinkedHashSet;
import java.util.Objects;
import org.checkerframework.javacutil.BugInCF;

/**
 * A set that is more efficient than HashSet for 0 and 1 elements. Uses {@code Objects.equals} for
 * object comparison and a {@link LinkedHashSet} for backing storage.
 */
public final class MostlySingleton<T extends Object> extends AbstractMostlySingleton<T> {

    /** Create a MostlySingleton. */
    public MostlySingleton() {
        super(State.EMPTY);
    }

    /** Create a MostlySingleton. */
    public MostlySingleton(T value) {
        super(State.SINGLETON, value);
    }

    @Override
    public boolean add(T e) {
        switch (state) {
            case EMPTY:
                state = State.SINGLETON;
                value = e;
                return true;
            case SINGLETON:
                if (value.equals(e)) {
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
        set = new LinkedHashSet<>();
        assert value != null : "@AssumeAssertion(nullness): previous add is non-null";
        set.add(value);
        value = null;
    }

    @Override
    public boolean contains(Object o) {
        switch (state) {
            case EMPTY:
                return false;
            case SINGLETON:
                return Objects.equals(o, value);
            case ANY:
                assert set != null : "@AssumeAssertion(nullness): set initialized before";
                return set.contains(o);
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }
}
