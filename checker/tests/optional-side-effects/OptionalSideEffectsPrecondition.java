import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.optional.qual.RequiresPresent;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

class OptionalSideEffectsPrecondition {

  void test1(OptionalContainer optContainer) {
    if (!optContainer.getOpt().isPresent()) {
      return;
    }
    List<String> strs = new ArrayList<>();
    methodA(optContainer, strs);
    optContainer.getOpt().get(); // OK
    bar(optContainer); // OK
  }

  void test2(OptionalContainer optContainer) {
    if (!optContainer.getOpt().isPresent()) {
      return;
    }
    List<String> strs = new ArrayList<>();
    methodB(optContainer, strs);

    // :: error: (contracts.precondition)
    bar(optContainer);
  }

  void test3(OptionalContainer optContainer) {
    if (!optContainer.getOpt().isPresent()) {
      return;
    }
    List<String> strs = new ArrayList<>();
    havoc(optContainer, strs);

    // :: error: (contracts.precondition)
    bar(optContainer);
  }

  @RequiresPresent("#1.getOpt()")
  void bar(OptionalContainer optContainer) {}

  @SideEffectsOnly("#2")
  void methodA(OptionalContainer optContainer, Object param) {}

  @SideEffectsOnly({"#1", "#2"})
  void methodB(OptionalContainer optContainer, Object param) {}

  @SideEffectsOnly({"#1.getOptional()"})
  // :: error: (flowexpr.parse.error)
  void methodC(OptionalContainer optContainer, Object param) {}

  void havoc(OptionalContainer optContainer, Object param) {}

  class OptionalContainer {

    @SuppressWarnings("optional:field")
    private Optional<String> opt;

    @SuppressWarnings("optional:parameter")
    OptionalContainer(Optional<String> opt) {
      this.opt = opt;
    }

    @Pure // Not required if running under -AassumePureGetters
    public Optional<String> getOpt() {
      return this.opt;
    }
  }
}
