import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.optional.qual.*;

class Main {

  @SuppressWarnings(
      "optional:parameter") // Don't care about this warning, unrelated to the test case
  void test(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      strs.forEach(s -> container.getOpt().get()); // Legal
    }
  }

  class OptContainer {

    @SuppressWarnings("optional:field") // Don't care about this warning, unrelated to the test case
    private Optional<String> opt;

    public OptContainer(String s) {
      this.opt = Optional.ofNullable(s);
    }

    public Optional<String> getOpt() {
      return this.opt;
    }
  }
}
