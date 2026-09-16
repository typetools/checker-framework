import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.checkerframework.checker.optional.qual.*;
import org.checkerframework.common.aliasing.qual.*;
import org.checkerframework.dataflow.qual.*;

class Main {

  void test(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      strs.forEach(s -> container.getOpt().get()); // Legal
    }
  }

  void test2(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      // :: error: [method.invocation]
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
            // :: error: [method.invocation]
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

  // Creating a function object is not deterministic, so a lambda whose body creates one is not
  // pure and the initial store loses what it knew about modifiable method call expressions.  All
  // three spellings of "create a function object" behave alike.

  void test5a(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      strs.forEach(
          s -> {
            Supplier<String> f = container::name;
            // :: error: [method.invocation]
            container.getOpt().get(); // Not ok
          });
    }
  }

  void test5b(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      strs.forEach(
          s -> {
            Supplier<String> f = () -> "x";
            // :: error: [method.invocation]
            container.getOpt().get(); // Not ok
          });
    }
  }

  void test5c(OptContainer container, List<String> strs) {
    if (container.getOpt().isPresent()) {
      strs.forEach(
          s -> {
            Supplier<String> f =
                new Supplier<String>() {
                  @Override
                  public String get() {
                    return "x";
                  }
                };
            // :: error: [method.invocation]
            container.getOpt().get(); // Not ok
          });
    }
  }

  void test6(OptContainer container) {
    if (container.getOpt().isPresent()) {
      // :: error: [method.invocation]
      container.forEach(s -> container.getOpt().get());
    }
  }

  void test7(OptContainer container) {
    if (container.getOpt().isPresent()) {
      container.forEachLeaksSome("Test", s -> container.getOpt().get()); // Legal
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

    <T> void forEach(Consumer<T> op) {
      // op may leak!
    }

    <T> void forEachLeaksSome(String s, @NonLeaked Consumer<T> op) {
      // s leaks, op does not
    }
  }
}
