import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

class OptionalBoxed {

  OptionalDouble aField;

  void m(OptionalInt aParam) {
    int x = OptionalLong.of(1L).hashCode();
    boolean b = Optional.empty().isPresent();
    OptionalDouble.empty().ifPresent(d -> {});
    boolean b4 = OptionalLong.empty().isPresent();
  }
}
