import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.checkerframework.checker.optional.qual.NonEmpty;
import org.checkerframework.checker.optional.qual.UnknownNonEmpty;

// Don't use these!  Use from the optional package instead.
// import org.checkerframework.checker.nonempty.qual.NonEmpty;
// import org.checkerframework.checker.nonempty.qual.UnknownNonEmpty;

class OptionalParameterTest {
  boolean a, b, c, d, e, f;

  public void findDatesByIds1(Stream<Integer> ids) {
    Function<Optional<Integer>, Stream<Integer>> f =
        (Optional<Integer> optional) -> {
          if (a) {
            // error
            return optional.map(Stream::of).orElse(Stream.empty());
          } else if (b) {
            // error:
            Optional<@NonEmpty Stream<Integer>> soi = optional.map(Stream::of);
            return soi.orElse(Stream.empty());
          } else if (c) {
            // no error:
            Optional<Stream<Integer>> soi = optional.map(Stream::of);
            return soi.orElse(Stream.empty());
          } else {
            // no error:
            Optional<@UnknownNonEmpty Stream<Integer>> soi = optional.map(Stream::of);
            return soi.orElse(Stream.empty());
          }
        };
  }

  public void findDatesByIds2(Stream<Integer> ids) {
    Function<Optional<Integer>, Stream<Integer>> f =
        (Optional<Integer> optional) -> {
          if (a) {
            // error
            return optional.map(Stream::of).orElseGet(Stream::empty);
          } else if (b) {
            // error:
            Optional<@NonEmpty Stream<Integer>> soi = optional.map(Stream::of);
            return soi.orElseGet(Stream::empty);
          } else if (c) {
            // no error:
            Optional<Stream<Integer>> soi = optional.map(Stream::of);
            return soi.orElseGet(Stream::empty);
          } else {
            // no error:
            Optional<@UnknownNonEmpty Stream<Integer>> soi = optional.map(Stream::of);
            return soi.orElseGet(Stream::empty);
          }
        };
  }
}
