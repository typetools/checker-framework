import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.optional.qual.*;
import org.checkerframework.dataflow.qual.*;

class OptionalSideEffectsLambda {

  void fooWithEnhancedFor(OptContainer container, List<String> strs) {
    if (!container.getOptStr().isPresent()) {
      return;
    }
    for (String s : strs) {
      bar(container); // OK
    }
  }

  void fooWithForEach(OptContainer container, List<String> strs) {
    if (!container.getOptStr().isPresent()) {
      return;
    }
    strs.forEach(s -> bar(container)); // OK
  }

  @RequiresPresent("#1.getOptStr()")
  @SideEffectFree
  void bar(OptContainer container) {}

  class OptContainer {

    @SuppressWarnings("optional:field")
    private Optional<String> optStr;

    OptContainer(String s) {
      this.optStr = Optional.ofNullable(s);
    }

    @Pure
    public Optional<String> getOptStr() {
      return this.optStr;
    }
  }
}
