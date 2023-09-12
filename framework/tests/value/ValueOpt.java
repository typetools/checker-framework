import java.util.Optional;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.common.value.qual.IntVal;

public class ValueOpt {

  Optional<@NonNegative Long> method(Optional<@IntVal(Long.MAX_VALUE) Long> opt1) {
    @NonNegative Long l = Long.MAX_VALUE;
    @NonNegative long l2 = -1l;
    Optional<@NonNegative Long> opt2 = opt1;
    Optional<@NonNegative Long> opt3 = Optional.<@IntVal(Long.MAX_VALUE) Long>of(Long.MAX_VALUE);
    return Optional.of(Long.MAX_VALUE);
  }
}
