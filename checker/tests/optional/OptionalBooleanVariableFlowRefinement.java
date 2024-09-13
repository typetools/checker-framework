import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.optional.qual.*;
import org.checkerframework.dataflow.qual.*;

@SuppressWarnings({"optional:parameter", "optional:field", "optional:collection"})
class OptionalBooleanVariableFlowRefinement {

  void validRefinementTest(Optional<String> opt) {
    boolean optIsPresent = opt.isPresent();
    if (optIsPresent) {
      opt.get(); // Legal
    }
  }

  void otherValidRefinement(OptContainer container) {
    boolean isGetLegal =
        container.getOptStrs().isPresent() && !container.getOptStrs().get().isEmpty();
    if (isGetLegal) {
      container.getOptStrs().get(); // Legal
    }
  }

  class OptContainer {
    private Optional<List<String>> strs;

    public OptContainer(List<String> strs) {
      this.strs = Optional.ofNullable(strs);
    }

    @Pure
    public Optional<List<String>> getOptStrs() {
      return this.strs;
    }
  }
}
