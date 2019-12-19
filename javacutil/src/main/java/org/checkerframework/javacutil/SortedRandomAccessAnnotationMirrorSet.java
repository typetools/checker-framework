package org.checkerframework.javacutil;

import java.util.*;
import javax.lang.model.element.AnnotationMirror;

public class SortedRandomAccessAnnotationMirrorSet
        implements List<AnnotationMirror>,
                Set<AnnotationMirror>,
                RandomAccess,
                RandomAccessSet<AnnotationMirror> {

    private ArrayList<AnnotationMirror> shadowList;

    public SortedRandomAccessAnnotationMirrorSet() {
        shadowList = new ArrayList<>();
    }

    public static Unmodifiable unmodifiable(Set<AnnotationMirror> set) {
        return new Unmodifiable((SortedRandomAccessAnnotationMirrorSet) set);
    }

    @Override
    public int size() {
        return shadowList.size();
    }

    @Override
    public boolean isEmpty() {
        return shadowList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public Iterator<AnnotationMirror> iterator() {
        return shadowList.iterator();
    }

    @Override
    public Object[] toArray() {
        return shadowList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return shadowList.toArray(ts);
    }

    @Override
    public boolean add(AnnotationMirror annotationMirror) {
        int index =
                Collections.binarySearch(
                        shadowList, annotationMirror, AnnotationUtils::compareAnnotationMirrors);
        // Already found, don't insert the same value
        if (index >= 0) {
            return false;
        }

        // index = -(insertion point) - 1
        int insertionPoint = -index - 1;
        shadowList.add(insertionPoint, annotationMirror);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof AnnotationMirror)) {
            return false;
        }

        int index =
                Collections.binarySearch(
                        shadowList,
                        (AnnotationMirror) o,
                        AnnotationUtils::compareAnnotationMirrors);
        if (index < 0) {
            return false;
        }

        shadowList.remove(index);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        for (Object el : collection) {
            if (indexOf(el) == -1) {
                return false;
            }
        }
        return true;
    }

    // O(n^2)
    // TODO: recheck
    @Override
    public boolean addAll(Collection<? extends AnnotationMirror> collection) {
        boolean changed = false;
        for (AnnotationMirror anno : collection) {
            changed = add(anno) || changed;
        }
        return changed;
    }

    // TODO: recheck
    @Override
    public boolean addAll(int i, Collection<? extends AnnotationMirror> collection) {
        // TODO: This is illegal
        throw new RuntimeException("Illegal operation");
    }

    // O(n^2)
    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean changed = false;
        for (Object val : collection) {
            changed = remove(val) || changed;
        }
        return changed;
    }

    // O(n^2)
    @Override
    public boolean retainAll(Collection<?> collection) {
        assert false;
        ArrayList<AnnotationMirror> toRetain = new ArrayList<>(collection.size());
        for (Object el : collection) {
            int index = indexOf(el);
            if (index != -1) {
                toRetain.add(shadowList.get(index));
            }
        }

        if (toRetain.size() == shadowList.size()) {
            return false;
        }

        toRetain.sort(AnnotationUtils::compareAnnotationMirrors);
        shadowList = toRetain;
        return true;
    }

    @Override
    public void clear() {
        shadowList.clear();
    }

    @Override
    public AnnotationMirror get(int i) {
        return shadowList.get(i);
    }

    @Override
    public AnnotationMirror set(int i, AnnotationMirror annotationMirror) {
        // TODO: This is illegal
        throw new RuntimeException("Illegal operation");
    }

    @Override
    public void add(int i, AnnotationMirror annotationMirror) {
        // TODO: This is illegal
        throw new RuntimeException("Illegal operation");
    }

    @Override
    public AnnotationMirror remove(int i) {
        // TODO: This is illegal
        throw new RuntimeException("Illegal operation");
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof AnnotationMirror)) {
            return -1;
        }

        int index =
                Collections.binarySearch(
                        shadowList,
                        (AnnotationMirror) o,
                        AnnotationUtils::compareAnnotationMirrors);
        return index < 0 ? -1 : index;
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public ListIterator<AnnotationMirror> listIterator() {
        assert false;
        return shadowList.listIterator();
    }

    @Override
    public ListIterator<AnnotationMirror> listIterator(int i) {
        assert false;
        return shadowList.listIterator(i);
    }

    @Override
    public List<AnnotationMirror> subList(int i1, int i2) {
        assert false;
        return shadowList.subList(i1, i2);
    }

    @Override
    public Spliterator<AnnotationMirror> spliterator() {
        assert false;
        return shadowList.spliterator();
    }

    @Override
    public String toString() {
        return shadowList.toString();
    }

    @Override
    public int hashCode() {
        assert false;
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Collection)) {
            return false;
        }

        Collection<?> c = (Collection<?>) o;
        if (c.size() != this.size()) {
            return false;
        }

        if (c instanceof SortedRandomAccessAnnotationMirrorSet) {
            SortedRandomAccessAnnotationMirrorSet s = (SortedRandomAccessAnnotationMirrorSet) c;
            for (int i = 0; i < s.size(); i++) {
                if (AnnotationUtils.compareAnnotationMirrors(get(i), s.get(i)) != 0) {
                    return false;
                }
            }
            return true;
        }

        return containsAll(c);
    }

    private static class Unmodifiable extends SortedRandomAccessAnnotationMirrorSet {
        private final SortedRandomAccessAnnotationMirrorSet set;

        private Unmodifiable(SortedRandomAccessAnnotationMirrorSet set) {
            this.set = set;
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return set.contains(o);
        }

        @Override
        public Iterator<AnnotationMirror> iterator() {
            return set.iterator();
        }

        @Override
        public Object[] toArray() {
            return set.toArray();
        }

        @Override
        public <T> T[] toArray(T[] ts) {
            return set.toArray(ts);
        }

        @Override
        public AnnotationMirror get(int i) {
            return set.get(i);
        }

        @Override
        public int indexOf(Object o) {
            return set.indexOf(o);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return set.containsAll(collection);
        }

        @Override
        public ListIterator<AnnotationMirror> listIterator() {
            return set.listIterator();
        }

        @Override
        public ListIterator<AnnotationMirror> listIterator(int i) {
            return set.listIterator(i);
        }

        @Override
        public List<AnnotationMirror> subList(int i1, int i2) {
            return set.subList(i1, i2);
        }

        @Override
        public Spliterator<AnnotationMirror> spliterator() {
            return set.spliterator();
        }

        @Override
        public String toString() {
            return set.toString();
        }

        @Override
        public boolean add(AnnotationMirror annotationMirror) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public boolean remove(Object o) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public boolean addAll(Collection<? extends AnnotationMirror> collection) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public boolean addAll(int i, Collection<? extends AnnotationMirror> collection) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public void clear() {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new RuntimeException("Not implemented");
        }
    }
}
