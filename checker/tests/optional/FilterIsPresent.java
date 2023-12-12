import java.util.Optional;
import java.util.stream.Stream;

public class FilterIsPresent {

  <E> Stream<E> filterPresent(Stream<Optional<E>> stream) {
    return stream.filter(Optional::isPresent).map(Optional::get);
  }
}
