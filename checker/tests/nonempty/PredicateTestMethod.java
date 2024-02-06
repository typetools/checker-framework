import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

class PredicateTestMethod {

  public static <T> List<T> filter1(Collection<T> coll, Predicate<? super T> filter) {
    List<T> result = new ArrayList<>();
    for (T elt : coll) {
      if (filter.test(elt)) {
        result.add(elt);
      }
    }
    return result;
  }
}
