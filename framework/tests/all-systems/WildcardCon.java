import java.util.Comparator;

@SuppressWarnings("all") // Only testing for crashes
public class WildcardCon<E> {
  ComparatorClass<ComparableClass<? extends Object>> RANGE_LEX_ORDERING = null;

  WildcardCon(Comparator<? super E> comparator) {}

  <C extends Comparable> void use() {
    new WildcardCon<ComparableClass<C>>(RANGE_LEX_ORDERING);
  }

  class ComparableClass<C extends Comparable> {}

  abstract class ComparatorClass<T> implements Comparator<T> {}
}
