import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

class OptionalBoxed {

  // :: warning: (optional.field)
  OptionalDouble aField;

  // :: warning: (optional.parameter)
  void m(OptionalInt aParam) {
    // :: warning: (introduce.eliminate)
    int x = OptionalLong.of(1L).hashCode();
    // :: warning: (introduce.eliminate)
    long y = OptionalLong.of(1L).orElse(2L);
    // :: warning: (introduce.eliminate)
    boolean b = Optional.empty().isPresent();
    // :: warning: (introduce.eliminate)
    OptionalDouble.empty().ifPresent(d -> {});
    // :: warning: (introduce.eliminate)
    boolean b4 = OptionalLong.empty().isPresent();
  }
}
