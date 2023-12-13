import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.optional.qual.Present;

public class OptionalMapMethodReference {
  Optional<String> getString() {
    return Optional.of("");
  }

  @Present Optional<Integer> method() {
    Optional<String> o = getString();
    @Present Optional<Integer> oInt;
    if (o.isPresent()) {
      // :: error: (assignment)
      oInt = o.map(this::convertNull);
      return o.map(this::convert);
    }
    return Optional.of(0);
  }

  @Nullable Integer convertNull(String s) {
    return null;
  }

  Integer convert(String s) {
    return 0;
  }
}
