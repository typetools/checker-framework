import java.math.BigInteger;
import java.util.Optional;
import org.checkerframework.checker.optional.qual.Present;

@SuppressWarnings("optional.parameter")
class PolyMap {

  void m(@Present Optional<Integer> arg) {
    BigInteger maxValue = arg.map(BigInteger::valueOf).get();
  }
}
