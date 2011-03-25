package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Collections {
  protected Collections() {}
  @SuppressWarnings("rawtypes")
  public final static Set EMPTY_SET = new HashSet();
  @SuppressWarnings("rawtypes")
  public final static List EMPTY_LIST = new LinkedList();
  @SuppressWarnings("rawtypes")
  public final static Map EMPTY_MAP = new HashMap();
  public static <T extends Comparable<? super T>> void sort(List<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> void sort(List<T> a1, @Nullable Comparator<? super T> a2) { throw new RuntimeException("skeleton method"); }
  public static <T> int binarySearch(List<? extends Comparable<? super T>> a2, T a3) { throw new RuntimeException("skeleton method"); }
  public static <T> int binarySearch(List<? extends T> a1, T a2, @Nullable Comparator<? super T> a3) { throw new RuntimeException("skeleton method"); }
  public static void reverse(List<?> a1) { throw new RuntimeException("skeleton method"); }
  public static void shuffle(List<?> a1) { throw new RuntimeException("skeleton method"); }
  public static void shuffle(List<?> a1, Random a2) { throw new RuntimeException("skeleton method"); }
  public static void swap(List<?> a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static <T> void fill(List<? super T> a1, T a2) { throw new RuntimeException("skeleton method"); }
  public static <T> void copy(List<? super T> a1, List<? extends T> a2) { throw new RuntimeException("skeleton method"); }
  public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> T min(Collection<? extends T> a1, @Nullable Comparator<? super T> a2) { throw new RuntimeException("skeleton method"); }
  public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> T max(Collection<? extends T> a1, @Nullable Comparator<? super T> a2) { throw new RuntimeException("skeleton method"); }
  public static void rotate(List<?> a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static <T> boolean replaceAll(List<T> a1, @Nullable T a2, T a3) { throw new RuntimeException("skeleton method"); }
  public static int indexOfSubList(List<?> a1, List<?> a2) { throw new RuntimeException("skeleton method"); }
  public static int lastIndexOfSubList(List<?> a1, List<?> a2) { throw new RuntimeException("skeleton method"); }
  public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> Set<T> unmodifiableSet(Set<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> SortedSet<T> unmodifiableSortedSet(SortedSet<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> List<T> unmodifiableList(List<? extends T> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> Collection<T> synchronizedCollection(Collection<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> Set<T> synchronizedSet(Set<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> SortedSet<T> synchronizedSortedSet(SortedSet<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> List<T> synchronizedList(List<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> Map<K, V> synchronizedMap(Map<K, V> a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> SortedMap<K, V> synchronizedSortedMap(SortedMap<K, V> a1) { throw new RuntimeException("skeleton method"); }
  public static <E> Collection<E> checkedCollection(Collection<E> a1, Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <E> Set<E> checkedSet(Set<E> a1, Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <E> SortedSet<E> checkedSortedSet(SortedSet<E> a1, Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <E> List<E> checkedList(List<E> a1, Class<E> a2) { throw new RuntimeException("skeleton method"); }
  public static <K, V> Map<K, V> checkedMap(Map<K, V> a1, Class<K> a2, Class<V> a3) { throw new RuntimeException("skeleton method"); }
  public static <K, V> SortedMap<K, V> checkedSortedMap(SortedMap<K, V> a1, Class<K> a2, Class<V> a3) { throw new RuntimeException("skeleton method"); }
  public final static <T> Set<T> emptySet() { throw new RuntimeException("skeleton method"); }
  public final static <T> List<T> emptyList() { throw new RuntimeException("skeleton method"); }
  public final static <K,V> Map<K, V> emptyMap() { throw new RuntimeException("skeleton method"); }
  public static <T> Set<T> singleton(T a1) { throw new RuntimeException("skeleton method"); }
  public static <T> List<T> singletonList(T a1) { throw new RuntimeException("skeleton method"); }
  public static <K, V> Map<K, V> singletonMap(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public static <T> List<T> nCopies(int a1, T a2) { throw new RuntimeException("skeleton method"); }
  public static <T> Comparator<T> reverseOrder() { throw new RuntimeException("skeleton method"); }
  public static <T> Comparator<T> reverseOrder(@Nullable Comparator<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> Enumeration<T> enumeration(Collection<T> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> ArrayList<T> list(Enumeration<T> a1) { throw new RuntimeException("skeleton method"); }
  public static int frequency(Collection<?> a1, @Nullable Object a2) { throw new RuntimeException("skeleton method"); }
  public static boolean disjoint(Collection<?> a1, Collection<?> a2) { throw new RuntimeException("skeleton method"); }
  // In JDK7, should instead be: @SafeVarargs
  @SuppressWarnings({"varargs","unchecked"})
  public static <T> boolean addAll(Collection<? super T> a1, T... a2) { throw new RuntimeException("skeleton method"); }
  public static <E> Set<E> newSetFromMap(Map<E, Boolean> a1) { throw new RuntimeException("skeleton method"); }
  public static <T> Queue<T> asLifoQueue(Deque<T> a1) { throw new RuntimeException("skeleton method"); }
}
