import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;
import org.checkerframework.checker.optional.qual.Present;

/**
 * Test case for rule #5: "If an Optional chain has a nested Optional chain, or has an intermediate
 * result of Optional, it's probably too complex."
 */
@SuppressWarnings("optional.parameter")
public class Marks5 {

  // Each method adds first and second, treating empty as zero, returning an Optional of the sum,
  // unless BOTH are empty, in which case return an empty Optional.

  Optional<BigDecimal> clever(Optional<BigDecimal> first, Optional<BigDecimal> second) {
    @SuppressWarnings({"methodref.inference.unimplemented", "methodref.receiver.invalid"})
    Optional<BigDecimal> result =
        Stream.of(first, second)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(BigDecimal::add);
    return result;
  }

  Optional<BigDecimal> clever2(Optional<BigDecimal> first, Optional<BigDecimal> second) {
    Stream<Optional<BigDecimal>> s = Stream.of(first, second);
    @SuppressWarnings("assignment.type.incompatible")
    Stream<@Present Optional<BigDecimal>> filtered =
        s.<Optional<BigDecimal>>filter(Optional::isPresent);
    Stream<BigDecimal> present = filtered.<BigDecimal>map(Optional::get);
    Optional<BigDecimal> result = present.reduce(BigDecimal::add);
    return result;
  }

  Optional<BigDecimal> moreClever(Optional<BigDecimal> first, Optional<BigDecimal> second) {
    Optional<BigDecimal> result =
        first.map(b -> second.map(b::add).orElse(b)).map(Optional::of).orElse(second);
    return result;
  }

  Optional<BigDecimal> clear(Optional<BigDecimal> first, Optional<BigDecimal> second) {
    Optional<BigDecimal> result;
    if (!first.isPresent() && !second.isPresent()) {
      result = Optional.empty();
    } else {
      result = Optional.of(first.orElse(BigDecimal.ZERO).add(second.orElse(BigDecimal.ZERO)));
    }
    return result;
  }
}
