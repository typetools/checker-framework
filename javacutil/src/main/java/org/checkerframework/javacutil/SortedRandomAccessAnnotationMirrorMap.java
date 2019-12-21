package org.checkerframework.javacutil;

import java.util.*;
import javax.lang.model.element.AnnotationMirror;

public class SortedRandomAccessAnnotationMirrorMap<V>
        implements Map<AnnotationMirror, V>, RandomAccess {

    @SuppressWarnings("serial")
    private static class SortedArraySet extends ArrayList<AnnotationMirror>
            implements RandomAccessSet<AnnotationMirror> {
        @Override
        public boolean contains(Object o) {
            if (!(o instanceof AnnotationMirror)) {
                return false;
            }

            return Collections.binarySearch(
                            this, (AnnotationMirror) o, AnnotationUtils::compareAnnotationMirrors)
                    >= 0;
        }
    }

    private SortedArraySet keys;
    private ArrayList<V> values;

    public SortedRandomAccessAnnotationMirrorMap() {
        keys = new SortedArraySet();
        values = new ArrayList<>();
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public boolean isEmpty() {
        return keys.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        if (!(o instanceof AnnotationMirror)) {
            return false;
        }

        return Collections.binarySearch(
                        keys, (AnnotationMirror) o, AnnotationUtils::compareAnnotationMirrors)
                >= 0;
    }

    @Override
    public boolean containsValue(Object o) {
        for (V value : values) {
            if (value.equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object o) {
        if (!(o instanceof AnnotationMirror)) {
            return null;
        }

        int index =
                Collections.binarySearch(
                        keys, (AnnotationMirror) o, AnnotationUtils::compareAnnotationMirrors);
        if (index >= 0) {
            return values.get(index);
        }
        return null;
    }

    @Override
    public V put(AnnotationMirror annotationMirror, V v) {
        int index =
                Collections.binarySearch(
                        keys, annotationMirror, AnnotationUtils::compareAnnotationMirrors);
        if (index >= 0) {
            V value = values.get(index);
            values.set(index, v);
            return value;
        }

        // index = -(insertion point) - 1
        int insertionPoint = -index - 1;
        keys.add(insertionPoint, annotationMirror);
        values.add(insertionPoint, v);

        return null;
    }

    @Override
    public V remove(Object o) {
        if (!(o instanceof AnnotationMirror)) {
            return null;
        }

        int index =
                Collections.binarySearch(
                        keys, (AnnotationMirror) o, AnnotationUtils::compareAnnotationMirrors);
        if (index >= 0) {
            V value = values.get(index);
            keys.remove(index);
            values.remove(index);
            return value;
        }

        return null;
    }

    @Override
    public void putAll(Map<? extends AnnotationMirror, ? extends V> map) {
        for (AnnotationMirror annotationMirror : map.keySet()) {
            put(annotationMirror, map.get(annotationMirror));
        }
    }

    @Override
    public void clear() {
        keys.clear();
        values.clear();
    }

    @Override
    public Set<AnnotationMirror> keySet() {
        return keys;
    }

    @Override
    public Collection<V> values() {
        return values;
    }

    @Override
    public Set<Entry<AnnotationMirror, V>> entrySet() {
        return createEntrySet();
    }

    private Set<Entry<AnnotationMirror, V>> createEntrySet() {
        LinkedHashSet<Entry<AnnotationMirror, V>> entries = new LinkedHashSet<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            entries.add(new InternalEntry(keys.get(i), values.get(i)));
        }
        return entries;
    }

    private class InternalEntry implements Entry<AnnotationMirror, V> {
        AnnotationMirror key;
        V value;

        InternalEntry(AnnotationMirror key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public AnnotationMirror getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            assert false : "Does not support setting value";
            return null;
        }
    }

    public static <V> Unmodifiable<V> unmodifiable(Map<AnnotationMirror, V> map) {
        return new Unmodifiable<>((SortedRandomAccessAnnotationMirrorMap<V>) map);
    }

    @SuppressWarnings("serial")
    private static class UnmodifiableKeys extends SortedArraySet {
        private final SortedArraySet set;

        private UnmodifiableKeys(SortedArraySet set) {
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

    private static class Unmodifiable<V> extends SortedRandomAccessAnnotationMirrorMap<V> {

        SortedRandomAccessAnnotationMirrorMap<V> map;

        Unmodifiable(SortedRandomAccessAnnotationMirrorMap<V> map) {
            this.map = map;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean containsKey(Object o) {
            return map.containsKey(o);
        }

        @Override
        public boolean containsValue(Object o) {
            return map.containsValue(o);
        }

        @Override
        public V get(Object o) {
            return map.get(o);
        }

        @Override
        public Set<AnnotationMirror> keySet() {
            return new UnmodifiableKeys(map.keys);
        }

        @Override
        public Collection<V> values() {
            return map.values();
        }

        @Override
        public Set<Entry<AnnotationMirror, V>> entrySet() {
            return map.entrySet();
        }

        @Override
        public V put(AnnotationMirror annotationMirror, V v) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public V remove(Object o) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public void putAll(Map<? extends AnnotationMirror, ? extends V> map) {
            throw new RuntimeException("Illegal operation");
        }

        @Override
        public void clear() {
            throw new RuntimeException("Illegal operation");
        }
    }
}
