package org.checkerframework.framework.util;

import static org.checkerframework.javacutil.AnnotationUtils.annotationOrdering;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Implements the Set interface for AnnotationMirrors that uses {@link
 * AnnotationUtils#areSame(AnnotationMirror, AnnotationMirror)} instead of {@link
 * AnnotationMirror#equals(Object)}.
 *
 * <p>AnnotationMirror is an interface and not all implementing classes provide a correct equals
 * method; therefore, the existing implementations of Set cannot be used.
 */
public class AnnotationMirrorSet implements Set<AnnotationMirror> {
    private Set<AnnotationMirror> shadowSet = new TreeSet<>(annotationOrdering());

    public AnnotationMirrorSet() {}

    public AnnotationMirrorSet(Collection<? extends AnnotationMirror> values) {
        this();
        this.addAll(values);
    }

    @Override
    public int size() {
        return shadowSet.size();
    }

    @Override
    public boolean isEmpty() {
        return shadowSet.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof AnnotationMirror
                && AnnotationUtils.containsSame(shadowSet, (AnnotationMirror) o);
    }

    @Override
    public Iterator<AnnotationMirror> iterator() {
        return shadowSet.iterator();
    }

    @Override
    public Object[] toArray() {
        return shadowSet.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return shadowSet.toArray(a);
    }

    @Override
    public boolean add(AnnotationMirror annotationMirror) {
        if (contains(annotationMirror)) {
            return false;
        }
        shadowSet.add(annotationMirror);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof AnnotationMirror) {
            AnnotationMirror found = null;
            for (AnnotationMirror am : shadowSet) {
                if (AnnotationUtils.areSame(am, (AnnotationMirror) o)) {
                    found = (AnnotationMirror) o;
                }
            }
            return found != null && shadowSet.remove(found);
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object a : c) {
            if (a instanceof AnnotationMirror) {
                if (!AnnotationUtils.containsSame(shadowSet, (AnnotationMirror) a)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends AnnotationMirror> c) {
        boolean result = true;
        for (AnnotationMirror a : c) {
            if (!add(a)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Set<AnnotationMirror> newSet = new TreeSet<>(annotationOrdering());
        for (Object o : c) {
            if (contains(o)) {
                newSet.add((AnnotationMirror) o);
            }
        }
        if (newSet.size() != shadowSet.size()) {
            shadowSet = newSet;
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = true;
        for (Object a : c) {
            if (!remove(a)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public void clear() {
        shadowSet.clear();
    }
}
