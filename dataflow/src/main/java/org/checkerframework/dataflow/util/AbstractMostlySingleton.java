package org.checkerframework.dataflow.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.javacutil.BugInCF;

/** Base class for sets that are more efficient than HashSet for 0 and 1 elements. */
public abstract class AbstractMostlySingleton<T extends Object> implements Set<T> {

    /** The possible states of the collection. */
    public enum State {
        /** An empty set. */
        EMPTY,
        /** A singleton set. */
        SINGLETON,
        /** A set of arbitrary size. */
        ANY
    }

    /** The current state. */
    protected State state;
    /** The current value, non-null when the state is SINGLETON. */
    protected @Nullable T value;
    /** The wrapped collection, non-null when the state is ANY. */
    protected @Nullable Collection<T> set;

    /** Create an AbstractMostlySingleton. */
    protected AbstractMostlySingleton(State s) {
        this.state = s;
        this.value = null;
    }

    /** Create an AbstractMostlySingleton. */
    protected AbstractMostlySingleton(State s, T v) {
        this.state = s;
        this.value = v;
    }

    @Override
    public int size() {
        switch (state) {
            case EMPTY:
                return 0;
            case SINGLETON:
                return 1;
            case ANY:
                assert set != null : "@AssumeAssertion(nullness): set initialized before";
                return set.size();
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator<T> iterator() {
        switch (state) {
            case EMPTY:
                return Collections.emptyIterator();
            case SINGLETON:
                return new Iterator<T>() {
                    private boolean hasNext = true;

                    @Override
                    public boolean hasNext() {
                        return hasNext;
                    }

                    @Override
                    public T next() {
                        if (hasNext) {
                            hasNext = false;
                            assert value != null
                                    : "@AssumeAssertion(nullness): previous add is non-null";
                            return value;
                        }
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            case ANY:
                assert set != null : "@AssumeAssertion(nullness): set initialized before";
                return set.iterator();
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }

    @Override
    public String toString() {
        switch (state) {
            case EMPTY:
                return "[]";
            case SINGLETON:
                return "[" + value + "]";
            case ANY:
                assert set != null : "@AssumeAssertion(nullness): set initialized before";
                return set.toString();
            default:
                throw new BugInCF("Unhandled state " + state);
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean res = false;
        for (T elem : c) {
            res |= add(elem);
        }
        return res;
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> @Nullable S @PolyNull [] toArray(S @PolyNull [] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(@Nullable Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
