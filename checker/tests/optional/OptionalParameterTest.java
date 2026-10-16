import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class OptionalParameterTest {
  public void findDatesByIds2(List<Integer> ids) {
    ids.stream()
        .map(Optional::ofNullable)
        .flatMap(optional -> optional.map(Stream::of).orElseGet(Stream::empty))
        .collect(Collectors.toList());
  }
}
