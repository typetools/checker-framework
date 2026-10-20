import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class CrashWithSuperWildcard<K> {
  ImmutableList<K> method(Map<K, Integer> map) {
    return sortKeysByValue(map, Ordering1.natural().reverse());
  }

  private static <K, V> ImmutableList<K> sortKeysByValue(
      Map<K, V> map, Comparator<? super V> valueComparator) {
    throw new RuntimeException();
  }

  static class ImmutableList<E> {}

  static class Ordering1<T> implements Comparator<T> {

    public static <C extends Comparable> Ordering1<C> natural() {
      throw new RuntimeException();
    }

    public <Z extends T> Ordering1<Z> reverse() {
      throw new RuntimeException();
    }

    @Override
    public int compare(T o1, T o2) {
      return 0;
    }
  }

  private static <K, V> ImmutableList<K> sortKeysByValue(
      Set<V> map, Comparator<? super V> valueComparator) {
    throw new RuntimeException();
  }

  ImmutableList<K> method2(Set<Integer> map) {
    return sortKeysByValue(map, Ordering2.natural().reverse());
  }

  static class Ordering2<T> implements Comparator<T> {

    public static <C extends Comparable<?>> Ordering2<C> natural() {
      throw new RuntimeException();
    }

    public <Z extends T> Ordering2<Z> reverse() {
      throw new RuntimeException();
    }

    @Override
    public int compare(T o1, T o2) {
      return 0;
    }
  }
}
