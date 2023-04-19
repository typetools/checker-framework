import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class MemberReferenceInference {
  void clever2(
      Stream<Optional<BigDecimal>> taintedStream,
      Stream<Optional<@Untainted BigDecimal>> untaintedStream) {
    // :: error: (type.arguments.not.inferred)
    Stream<@Untainted BigDecimal> s = taintedStream.map(Optional::get);
    Stream<@Untainted BigDecimal> s2 = untaintedStream.map(Optional::get);
    Stream<@Tainted BigDecimal> s3 = taintedStream.map(Optional::get);
    Stream<@Tainted BigDecimal> s4 = untaintedStream.map(Optional::get);
  }
}
