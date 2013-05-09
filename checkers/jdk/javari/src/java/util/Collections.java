package java.util;
import checkers.javari.quals.*;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

public class Collections {
    private Collections() { throw new RuntimeException("skeleton method"); }
    public static <T extends Comparable<? super T>> void sort(List<T> list) { throw new RuntimeException("skeleton method"); }
    public static <T> void sort(List<T> list,  Comparator<? super T> c) { throw new RuntimeException("skeleton method"); }
    public static <T> int binarySearch(@ReadOnly List< ? extends Comparable<? super T>> list, @ReadOnly T key) { throw new RuntimeException("skeleton method"); }
    public static <T> int binarySearch(@ReadOnly List<? extends T> list, @ReadOnly T key, Comparator<? super T> c) { throw new RuntimeException("skeleton method"); }
    public static void reverse(List<?> list) { throw new RuntimeException("skeleton method"); }
    public static void shuffle(List<?> list) { throw new RuntimeException("skeleton method"); }
    public static void shuffle(List<?> list,  Random rnd) { throw new RuntimeException("skeleton method"); }
    public static void swap(List<?> list, int i, int j) { throw new RuntimeException("skeleton method"); }
    public static <T> void fill(List<? super T> list, T obj) { throw new RuntimeException("skeleton method"); }
    public static <T> void copy(List<? super T> dest,  @ReadOnly List<? extends T> src) { throw new RuntimeException("skeleton method"); }
    public static <T extends @ReadOnly Object & Comparable<? super T>> T min(@ReadOnly Collection<? extends T> coll) { throw new RuntimeException("skeleton method"); }
    public static <T> T min(@ReadOnly Collection<? extends T> coll, Comparator<? super T> comp) { throw new RuntimeException("skeleton method"); }
    public static <T extends @ReadOnly Object & Comparable<? super T>> T max(@ReadOnly Collection<? extends T> coll) { throw new RuntimeException("skeleton method"); }
    public static <T> T max(@ReadOnly Collection<? extends T> coll,  Comparator<? super T> comp) { throw new RuntimeException("skeleton method"); }
    public static void rotate(List<?> list, int distance) { throw new RuntimeException("skeleton method"); }
    public static <T> boolean replaceAll(List<T> list, T oldVal, T newVal) { throw new RuntimeException("skeleton method"); }
    public static int indexOfSubList(@ReadOnly List<?> source, @ReadOnly List<?> target) { throw new RuntimeException("skeleton method"); }
    public static int lastIndexOfSubList(@ReadOnly List<?> source, @ReadOnly List<?> target) { throw new RuntimeException("skeleton method"); }
    public static <T>  @ReadOnly Collection<T> unmodifiableCollection(@ReadOnly Collection<? extends T> c) { throw new RuntimeException("skeleton method"); }
    public static <T> @ReadOnly Set<T> unmodifiableSet(@ReadOnly Set<? extends T> s) { throw new RuntimeException("skeleton method"); }
    public static <T>  @ReadOnly SortedSet<T> unmodifiableSortedSet(@ReadOnly SortedSet<T> s) { throw new RuntimeException("skeleton method"); }
    public static <T> @ReadOnly List<T> unmodifiableList(@ReadOnly List<? extends T> list) { throw new RuntimeException("skeleton method"); }
    public static <K,V> @ReadOnly Map<K,V> unmodifiableMap(@ReadOnly Map<? extends K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
    public static <K,V> @ReadOnly SortedMap<K,V> unmodifiableSortedMap(@ReadOnly SortedMap<K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
    public static <T> @PolyRead Collection<T> synchronizedCollection(@PolyRead Collection<T> c) { throw new RuntimeException("skeleton method"); }
    public static <T> @PolyRead Set<T> synchronizedSet(@PolyRead Set<T> s) { throw new RuntimeException("skeleton method"); }
    public static <T> @PolyRead SortedSet<T> synchronizedSortedSet(@PolyRead SortedSet<T> s) { throw new RuntimeException("skeleton method"); }
    public static <T> @PolyRead List<T> synchronizedList(@PolyRead List<T> list) { throw new RuntimeException("skeleton method"); }
    public static <K,V> @PolyRead Map<K,V> synchronizedMap(@PolyRead Map<K,V> m) { throw new RuntimeException("skeleton method"); }
    public static <K,V> @PolyRead SortedMap<K,V> synchronizedSortedMap(@PolyRead SortedMap<K,V> m) { throw new RuntimeException("skeleton method"); }
    public static <E> @PolyRead Collection<E> checkedCollection(@PolyRead Collection<E> c, Class<E> type) { throw new RuntimeException("skeleton method"); }
    public static <E> @PolyRead Set<E> checkedSet(@PolyRead Set<E> s, Class<E> type) { throw new RuntimeException("skeleton method"); }
    public static <E> @PolyRead SortedSet<E> checkedSortedSet(@PolyRead SortedSet<E> s, Class<E> type) { throw new RuntimeException("skeleton method"); }
    public static <E> @PolyRead List<E> checkedList(@PolyRead List<E> list, Class<E> type) { throw new RuntimeException("skeleton method"); }
    public static <K, V> @PolyRead Map<K, V> checkedMap(@PolyRead Map<K, V> m, Class<K> keyType, Class<V> valueType) { throw new RuntimeException("skeleton method"); }
    public static <K,V> @PolyRead SortedMap<K,V> checkedSortedMap(@PolyRead SortedMap<K, V> m, Class<K> keyType, Class<V> valueType) { throw new RuntimeException("skeleton method"); }
    @SuppressWarnings("rawtypes") public static final @ReadOnly Set EMPTY_SET = null;
    public static final <T> @ReadOnly Set<T> emptySet() { throw new RuntimeException("skeleton method"); }
    @SuppressWarnings("rawtypes") public static final @ReadOnly List EMPTY_LIST = null;
    public static final <T> @ReadOnly List<T> emptyList() { throw new RuntimeException("skeleton method"); }
    @SuppressWarnings("rawtypes") public static final @ReadOnly Map EMPTY_MAP = null;
    public static final <K,V>  @ReadOnly Map<K,V> emptyMap() { throw new RuntimeException("skeleton method"); }
    public static <T> @ReadOnly Set<T> singleton(T o) { throw new RuntimeException("skeleton method"); }
    public static <T> @ReadOnly List<T> singletonList(T o) { throw new RuntimeException("skeleton method"); }
    public static <K,V> @ReadOnly Map<K,V> singletonMap(K key, V value) { throw new RuntimeException("skeleton method"); }
    public static <T> @ReadOnly List<T> nCopies(int n, T o) { throw new RuntimeException("skeleton method"); }
    public static <T> Comparator<T> reverseOrder() { throw new RuntimeException("skeleton method"); }
    public static <T> Comparator<T> reverseOrder(Comparator<T> cmp) { throw new RuntimeException("skeleton method"); }
    public static <T> @ReadOnly Enumeration<T> enumeration(final @ReadOnly Collection<T> c) { throw new RuntimeException("skeleton method"); }
    public static <T> @PolyRead ArrayList<T> list(@PolyRead Enumeration<T> e) { throw new RuntimeException("skeleton method"); }
    public static int frequency(@ReadOnly Collection<?> c, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public static boolean disjoint(@ReadOnly Collection<?> c1, @ReadOnly Collection<?> c2) { throw new RuntimeException("skeleton method"); }
    // In JDK7, should instead be: @SafeVarargs
    @SuppressWarnings({"varargs","unchecked"})
    public static <T> boolean addAll(Collection<? super T> c, T... elements) { throw new RuntimeException("skeleton method"); }
    public static <E> @PolyRead Set<E> newSetFromMap(@PolyRead Map<E, Boolean> map) { throw new RuntimeException("skeleton method"); }
    public static <T> @PolyRead Queue<T> asLifoQueue(@PolyRead Deque<T> deque) { throw new RuntimeException("skeleton method"); }
}
