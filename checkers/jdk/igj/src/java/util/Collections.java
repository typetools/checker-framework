package java.util;
import checkers.igj.quals.*;

public class Collections{
  protected Collections(@ReadOnly Collections this) {}
  @SuppressWarnings("rawtypes")
  public final static @Immutable Set EMPTY_SET = null;
@SuppressWarnings("rawtypes")
  public final static @Immutable List EMPTY_LIST = null;
@SuppressWarnings("rawtypes")
  public final static @Immutable Map EMPTY_MAP = null;
  public static <T extends @ReadOnly Comparable<? super T>> void sort(@Mutable List<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> void sort(@Mutable List<T> a1, @ReadOnly Comparator<? super T> a2) { throw new RuntimeException("skeleton method"); }
  public static <T> int binarySearch(@ReadOnly List<? extends @ReadOnly Comparable<? super T>> a2, T a3) { throw new RuntimeException("skeleton method"); }
  public static <T> int binarySearch(@ReadOnly List<? extends T> a1, T a2, @ReadOnly Comparator<? super T> a3) { throw new RuntimeException("skeleton method"); }
  public static void reverse(@Mutable List<?> a1) { throw new RuntimeException("skeleton method"); }
  public static void shuffle(@Mutable List<?> a1) { throw new RuntimeException("skeleton method"); }
  public static void shuffle(@Mutable List<?> a1, Random a2) { throw new RuntimeException("skeleton method"); }
  public static void swap(@Mutable List<?> a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static <T> void fill(@Mutable List<? super T> a1, T a2) { throw new RuntimeException("skeleton method"); }
  public static <T> void copy(@Mutable List<? super T> a1, @ReadOnly List<? extends T> a2) { throw new RuntimeException("skeleton method"); }
  public static <T extends Object & Comparable<? super T>> T min(@ReadOnly Collection<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> T min(@ReadOnly Collection<? extends T> a1, @ReadOnly Comparator<? super T> a2) { throw new RuntimeException("skeleton method"); }
  public static <T extends Object & Comparable<? super T>> T max(@ReadOnly Collection<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> T max(@ReadOnly Collection<? extends T> a1, @ReadOnly Comparator<? super T> a2) { throw new RuntimeException("skeleton method"); }
  public static void rotate(@Mutable List<?> a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static <T> boolean replaceAll(@Mutable List<T> a1, T a2, T a3) { throw new RuntimeException("skeleton method"); }
  public static int indexOfSubList(@ReadOnly List<?> a1, @ReadOnly List<?> a2) { throw new RuntimeException("skeleton method"); }
  public static int lastIndexOfSubList(List<?> a1, List<?> a2) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly Collection<T> unmodifiableCollection(@ReadOnly Collection<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly Set<T> unmodifiableSet(@ReadOnly Set<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly SortedSet<T> unmodifiableSortedSet(@ReadOnly SortedSet<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly List<T> unmodifiableList(@ReadOnly List<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @ReadOnly Map<K, V> unmodifiableMap(@ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @ReadOnly SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @I Collection<T> synchronizedCollection(@I Collection<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @I Set<T> synchronizedSet(@I Set<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @I SortedSet<T> synchronizedSortedSet(@I SortedSet<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @I List<T> synchronizedList(@I List<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @I Map<K, V> synchronizedMap(@I Map<K, V> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @I SortedMap<K, V> synchronizedSortedMap(@I SortedMap<K, V> a1) { throw new RuntimeException("skeleton method"); }
  public static <E> @I Collection<E> checkedCollection(@I Collection<E> a1, Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <E> @I Set<E> checkedSet(@I Set<E> a1, Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <E> @I SortedSet<E> checkedSortedSet(@I SortedSet<E> a1, Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <E> @I List<E> checkedList(@I List<E> a1, Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @I Map<K, V> checkedMap(@I Map<K, V> a1, Class<K> a2, Class<V> a3) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @I SortedMap<K, V> checkedSortedMap(@I SortedMap<K, V> a1, Class<K> a2, Class<V> a3) { throw new RuntimeException("skeleton method"); }
  public final static <T> @Immutable Set<T> emptySet() { throw new RuntimeException("skeleton method"); }
  public final static <T> @Immutable List<T> emptyList() { throw new RuntimeException("skeleton method"); }
  public final static <K, V> @Immutable Map<K, V> emptyMap() { throw new RuntimeException("skeleton method"); }
  public static <T> @Immutable Set<T> singleton(T a1) { throw new RuntimeException("skeleton method"); }
  public static <T> @Immutable List<T> singletonList(T a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> @Immutable Map<K, V> singletonMap(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public static <T> @Immutable List<T> nCopies(int a1, T a2) { throw new RuntimeException("skeleton method"); }
  public static <T> @ReadOnly Comparator<T> reverseOrder() { throw new RuntimeException("skeleton method"); }
  public static <T> @I Comparator<T> reverseOrder(@I Comparator<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> Enumeration<T> enumeration(Collection<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> ArrayList<T> list(Enumeration<T> a1) { throw new RuntimeException("skeleton method"); }
  public static int frequency(@ReadOnly Collection<?> a1, @ReadOnly Object a2) { throw new RuntimeException("skeleton method"); }
  public static boolean disjoint(@ReadOnly Collection<?> a1, @ReadOnly Collection<?> a2) { throw new RuntimeException("skeleton method"); }
  // In JDK7, should instead be: @SafeVarargs
  @SuppressWarnings({"varargs","unchecked"})
  public static <T> boolean addAll(@Mutable Collection<? super T> a1, T @ReadOnly... a2) { throw new RuntimeException("skeleton method"); }
  public static <E> @I Set<E> newSetFromMap(@I Map<E, Boolean> a2) { throw new RuntimeException("skeleton method"); }
  public static <T> @I Queue<T> asLifoQueue(@I Deque<T> a1) { throw new RuntimeException("skeleton method"); }
}
