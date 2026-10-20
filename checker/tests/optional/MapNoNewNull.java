import java.math.BigInteger;
import java.util.Optional;

class MapNoNewNull {

  @SuppressWarnings("optional.parameter")
  void m(Optional<Digits> digitsAnnotation) {
    if (digitsAnnotation.isPresent()) {
      BigInteger maxValue = digitsAnnotation.map(Digits::integer).map(BigInteger::valueOf).get();
    }
  }
}

@interface Digits {
  public int integer();
}
