package java.util;
import checkers.igj.quals.*;

public class Collections{
  protected Collections() @ReadOnly {}
  public final static @Immutable java.util.Set EMPTY_SET = null;
  public final static @Immutable java.util.List EMPTY_LIST = null;
  public final static @Immutable java.util.Map EMPTY_MAP = null;
  public static <T extends @ReadOnly java.lang.Comparable<? super T>> void sort(@Mutable java.util.List<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> void sort(@Mutable java.util.List<T> a1, @ReadOnly java.util.Comparator<? super T> a2) { throw new RuntimeException("skeleton method"); }
  public static <T> int binarySearch(@ReadOnly java.util.List<? extends @ReadOnly java.lang.Comparable<? super T>> a2, T a3) { throw new RuntimeException("skeleton method"); }
  public static <T> int binarySearch(@ReadOnly java.util.List<? extends T> a1, T a2, @ReadOnly java.util.Comparator<? super T> a3) { throw new RuntimeException("skeleton method"); }
  public static void reverse(@Mutable java.util.List<?> a1) { throw new RuntimeException("skeleton method"); }
  public static void shuffle(@Mutable java.util.List<?> a1) { throw new RuntimeException("skeleton method"); }
  public static void shuffle(@Mutable java.util.List<?> a1, java.util.Random a2) { throw new RuntimeException("skeleton method"); }
  public static void swap(@Mutable java.util.List<?> a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static <T> void fill(@Mutable java.util.List<? super T> a1, T a2) { throw new RuntimeException("skeleton method"); }
  public static <T> void copy(@Mutable java.util.List<? super T> a1, @ReadOnly java.util.List<? extends T> a2) { throw new RuntimeException("skeleton method"); }
  public static <T extends Object & java.lang.Comparable<? super T>> T min(@ReadOnly java.util.Collection<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> T min(@ReadOnly java.util.Collection<? extends T> a1, @ReadOnly java.util.Comparator<? super T> a2) { throw new RuntimeException("skeleton method"); }
  public static <T extends Object & java.lang.Comparable<? super T>> T max(@ReadOnly java.util.Collection<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> T max(@ReadOnly java.util.Collection<? extends T> a1, @ReadOnly java.util.Comparator<? super T> a2) { throw new RuntimeException("skeleton method"); }
  public static void rotate(@Mutable java.util.List<?> a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static <T> boolean replaceAll(@Mutable java.util.List<T> a1, T a2, T a3) { throw new RuntimeException("skeleton method"); }
  public static int indexOfSubList(@ReadOnly java.util.List<?> a1, @ReadOnly java.util.List<?> a2) { throw new RuntimeException("skeleton method"); }
  public static int lastIndexOfSubList(java.util.List<?> a1, java.util.List<?> a2) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly java.util.Collection<T> unmodifiableCollection(@ReadOnly java.util.Collection<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly java.util.Set<T> unmodifiableSet(@ReadOnly java.util.Set<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly java.util.SortedSet<T> unmodifiableSortedSet(@ReadOnly java.util.SortedSet<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly java.util.List<T> unmodifiableList(@ReadOnly java.util.List<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @ReadOnly java.util.Map<K, V> unmodifiableMap(@ReadOnly java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @ReadOnly java.util.SortedMap<K, V> unmodifiableSortedMap(java.util.SortedMap<K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @I java.util.Collection<T> synchronizedCollection(@I java.util.Collection<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @I java.util.Set<T> synchronizedSet(@I java.util.Set<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @I java.util.SortedSet<T> synchronizedSortedSet(@I java.util.SortedSet<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @I java.util.List<T> synchronizedList(@I java.util.List<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @I java.util.Map<K, V> synchronizedMap(@I java.util.Map<K, V> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @I java.util.SortedMap<K, V> synchronizedSortedMap(@I java.util.SortedMap<K, V> a1) { throw new RuntimeException("skeleton method"); }
  public static <E> @I java.util.Collection<E> checkedCollection(@I java.util.Collection<E> a1, java.lang.Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <E> @I java.util.Set<E> checkedSet(@I java.util.Set<E> a1, java.lang.Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <E> @I java.util.SortedSet<E> checkedSortedSet(@I java.util.SortedSet<E> a1, java.lang.Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <E> @I java.util.List<E> checkedList(@I java.util.List<E> a1, java.lang.Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @I java.util.Map<K, V> checkedMap(@I java.util.Map<K, V> a1, java.lang.Class<K> a2, java.lang.Class<V> a3) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @I java.util.SortedMap<K, V> checkedSortedMap(@I java.util.SortedMap<K, V> a1, java.lang.Class<K> a2, java.lang.Class<V> a3) { throw new RuntimeException("skeleton method"); }
  public final static <T> @Immutable java.util.Set<T> emptySet() { throw new RuntimeException("skeleton method"); }
  public final static <T> @Immutable java.util.List<T> emptyList() { throw new RuntimeException("skeleton method"); }
  public final static <K, V> @Immutable java.util.Map<K, V> emptyMap() { throw new RuntimeException("skeleton method"); }
  public static <T> @Immutable java.util.Set<T> singleton(T a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @Immutable java.util.List<T> singletonList(T a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @Immutable java.util.Map<K, V> singletonMap(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public static <T> @Immutable java.util.List<T> nCopies(int a1, T a2) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly java.util.Comparator<T> reverseOrder() { throw new RuntimeException("skeleton method"); }
  public static <T> @I java.util.Comparator<T> reverseOrder(@I java.util.Comparator<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> java.util.Enumeration<T> enumeration(java.util.Collection<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> java.util.ArrayList<T> list(java.util.Enumeration<T> a1) { throw new RuntimeException("skeleton method"); }
  public static int frequency(@ReadOnly java.util.Collection<?> a1, @ReadOnly java.lang.Object a2) { throw new RuntimeException("skeleton method"); }
  public static boolean disjoint(@ReadOnly java.util.Collection<?> a1, @ReadOnly java.util.Collection<?> a2) { throw new RuntimeException("skeleton method"); }
  public static <T> boolean addAll(@Mutable java.util.Collection<? super T> a1, T @ReadOnly... a2) { throw new RuntimeException("skeleton method"); }
  public static <E> @I java.util.Set<E> newSetFromMap(@I java.util.Map<E, java.lang.Boolean> a2) { throw new RuntimeException("skeleton method"); }
  public static <T> @I java.util.Queue<T> asLifoQueue(@I java.util.Deque<T> a1) { throw new RuntimeException("skeleton method"); }
}
