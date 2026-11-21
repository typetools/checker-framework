// A method reference to `iterator()` or `List.copyOf` is equivalent to the corresponding lambda.

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MethodReferenceToIterator {

  void lambdas(List<List<String>> lists) {
    lists.stream().map(l -> l.iterator()).count();
    lists.stream().map(l -> List.copyOf(l)).count();
  }

  void methodReferences(List<List<String>> lists) {
    lists.stream().map(List::iterator).count();
    lists.stream().map(Collection::iterator).collect(Collectors.toList());
    lists.stream().map(List::copyOf).count();
  }

  static <T> void iterableMethodReference(Stream<? extends Iterable<? extends T>> iterables) {
    iterables.map(Iterable::iterator);
  }
}
