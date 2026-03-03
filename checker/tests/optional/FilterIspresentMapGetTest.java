import java.util.Optional;
import java.util.stream.Stream;

class FilterIspresentMapGetTest {

  void m(Stream<Optional<String>> ss) {
    ss.filter(Optional::isPresent).map(Optional::get);
  }
}
