import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

// @skip-test
public class Issue6725 {
  static <T> Iterable<T> prefix(Collection<? extends Iterable<? extends T>> iterables) {
    Collection<? extends Iterator<? extends T>> iterators =
        iterables.stream().map(Iterable::iterator).collect(Collectors.toList());
    // ...
    return List.of();
  }
}
