import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.optional.qual.*;
import org.checkerframework.dataflow.qual.*;

class Main {

  void test(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      strs.forEach(s -> container.getOpt().get()); // Legal
    }
  }

  void test2(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      // :: error: (method.invocation)
      strs.forEach(s -> container.getOptImpure().get());
    }
  }

  void test3(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      strs.forEach(
          s -> {
            container.getOpt().get(); // OK
          });
    }
  }

  void test4(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      strs.forEach(
          s -> {
            container.sideEffect();
            // :: error: (method.invocation)
            container.getOpt().get(); // Not ok
          });
    }
  }

  void test5(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      strs.forEach(
          s -> {
            container.getOpt();
            container.name();
            container.getOpt().get(); // OK
          });
    }
  }

  class OptContainer {

    @SuppressWarnings("optional:field") // Don't care about this warning, unrelated to the test case
    private Optional<String> opt;

    public OptContainer(String s) {
      this.opt = Optional.ofNullable(s);
    }

    @Pure
    public Optional<String> getOpt() {
      return this.opt;
    }

    @Pure
    public String name() {
      return "name";
    }

    public Optional<String> getOptImpure() {
      System.out.println("Side effect");
      return this.opt;
    }

    public void sideEffect() {}
  }
}
