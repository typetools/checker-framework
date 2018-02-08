package org.checkerframework.dataflow.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public abstract class AbstractMostlySingleton<T> implements Set<T> {

    public enum State {
        EMPTY,
        SINGLETON,
        ANY
    }

    protected State state;
    protected T value;
    protected Collection<T> set;

    @Override
    public int size() {
        switch (state) {
            case EMPTY:
                return 0;
            case SINGLETON:
                return 1;
            case ANY:
                return set.size();
            default:
                throw new AssertionError();
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
                return set.iterator();
            default:
                throw new AssertionError();
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
                return set.toString();
            default:
                throw new AssertionError();
        }
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> S[] toArray(S[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
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
