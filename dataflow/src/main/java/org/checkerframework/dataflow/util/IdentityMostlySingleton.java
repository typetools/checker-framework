package org.checkerframework.dataflow.util;

import java.util.ArrayList;
import org.checkerframework.javacutil.BugInCF;

/**
 * A set that is more efficient than HashSet for 0 and 1 elements. Uses objects identity for object
 * comparison and an {@link ArrayList} for backing storage.
 */
public final class IdentityMostlySingleton<T> extends AbstractMostlySingleton<T> {

    public IdentityMostlySingleton() {
        this.state = State.EMPTY;
    }

    public IdentityMostlySingleton(T value) {
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
                set = new ArrayList<>();
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
                return o == value;
            case ANY:
                return set.contains(o);
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }
}
