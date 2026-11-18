// An override may not assume that its functional-interface argument is pure when a declaration
// that it overrides makes no promise: a call through that declaration checks nothing.

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.checkerframework.common.aliasing.qual.NonLeaked;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityOverrideOfUnannotated {

  static class OptContainer {
    @SuppressWarnings("optional:field") // unrelated to the test
    private Optional<String> opt = Optional.of("x");

    @Pure
    Optional<String> getOpt() {
      return opt;
    }

    void clear() {
      opt = Optional.empty();
    }
  }

  @SideEffectFree
  static void each(List<String> l, @NonLeaked Consumer<String> f) {
    for (String s : l) {
      f.accept(s);
    }
  }

  static class A {
    String run(OptContainer c, List<String> l, Consumer<String> op) {
      return "";
    }
  }

  static class B extends A {
    @Override
    @SideEffectFree
    String run(OptContainer c, List<String> l, Consumer<String> op) {
      if (c.getOpt().isPresent()) {
        each(
            l,
            s -> {
              // On an earlier iteration, op.accept may have emptied c's Optional.
              // :: error: [method.invocation]
              c.getOpt().get();
              op.accept(s);
            });
      }
      return "";
    }
  }

  void caller(OptContainer c, List<String> l) {
    A a = new B();
    a.run(c, l, s -> c.clear());
  }
}
