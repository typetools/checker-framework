package org.checkerframework.javacutil;

import java.util.*;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class SortedRandomAccessAnnotationMirrorSet implements RandomAccessSet<AnnotationMirror> {

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
    public boolean contains(@Nullable Object o) {
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
    public <T> @Nullable T @PolyNull [] toArray(T @PolyNull [] a) {
        return shadowList.toArray(a);
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
    public boolean remove(@Nullable Object o) {
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
    @Override
    public boolean addAll(Collection<? extends AnnotationMirror> collection) {
        boolean changed = false;
        for (AnnotationMirror anno : collection) {
            changed = add(anno) || changed;
        }
        return changed;
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

    protected AnnotationMirror get(int i) {
        return shadowList.get(i);
    }

    private int indexOf(@Nullable Object o) {
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
    public Spliterator<AnnotationMirror> spliterator() {
        return shadowList.spliterator();
    }

    @Override
    public String toString() {
        return shadowList.toString();
    }

    @Override
    public int hashCode() {
        return shadowList.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Collection)) {
            return false;
        }

        Collection<?> otherCollection = (Collection<?>) other;
        if (otherCollection.size() != size()) {
            return false;
        }

        if (otherCollection instanceof SortedRandomAccessAnnotationMirrorSet) {
            SortedRandomAccessAnnotationMirrorSet otherSet =
                    (SortedRandomAccessAnnotationMirrorSet) otherCollection;
            for (int i = 0; i < otherSet.size(); i++) {
                if (AnnotationUtils.compareAnnotationMirrors(get(i), otherSet.get(i)) != 0) {
                    return false;
                }
            }
            return true;
        }

        return containsAll(otherCollection);
    }

    private static class Unmodifiable extends SortedRandomAccessAnnotationMirrorSet {
        private final SortedRandomAccessAnnotationMirrorSet set;

        private Unmodifiable(SortedRandomAccessAnnotationMirrorSet set) {
            this.set = set;
        }

        @Override
        protected AnnotationMirror get(int i) {
            return set.get(i);
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
        public boolean contains(@Nullable Object o) {
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
        public <T> @Nullable T @PolyNull [] toArray(T @PolyNull [] a) {
            return set.toArray(a);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return set.containsAll(collection);
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
        public boolean remove(@Nullable Object o) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public boolean addAll(Collection<? extends AnnotationMirror> collection) {
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
    }
}
