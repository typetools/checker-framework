import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.optional.qual.RequiresPresent;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

class OptionalSideEffectsLambda {

  void fooWithEnhancedFor(OptContainer container, List<String> strs) {
    if (!container.getOptStr().isPresent()) {
      return;
    }
    for (String s : strs) {
      // This should verify because the call to Iterator.next only side effects the iterator.
      bar(container);
    }
  }

  void fooWithForEach(OptContainer container, List<String> strs) {
    if (!container.getOptStr().isPresent()) {
      return;
    }
    strs.forEach(s -> bar(container));
  }

  @RequiresPresent("#1.getOptStr()")
  @SideEffectFree
  void bar(OptContainer container) {}
}

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
