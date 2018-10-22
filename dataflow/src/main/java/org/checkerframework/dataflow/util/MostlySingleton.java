package org.checkerframework.dataflow.util;

import java.util.HashSet;
import java.util.Objects;
import org.checkerframework.javacutil.BugInCF;

/**
 * A set that is more efficient than HashSet for 0 and 1 elements. Uses {@code Objects.equals} for
 * object comparison and a {@link HashSet} for backing storage.
 */
public final class MostlySingleton<T> extends AbstractMostlySingleton<T> {

    public MostlySingleton() {
        this.state = State.EMPTY;
    }

    public MostlySingleton(T value) {
        this.state = State.SINGLETON;
        this.value = value;
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
                state = State.ANY;
                set = new HashSet<>();
                set.add(value);
                value = null;
                // fallthrough
            case ANY:
                return set.add(e);
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }

    @Override
    public boolean contains(Object o) {
        switch (state) {
            case EMPTY:
                return false;
            case SINGLETON:
                return Objects.equals(o, value);
            case ANY:
                return set.contains(o);
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }
}
