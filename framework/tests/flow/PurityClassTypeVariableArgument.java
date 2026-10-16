import java.util.function.Supplier;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * An argument to a parameter whose declared type is a type variable is not checked at the call
 * site, because no implementation may assume anything about it.
 */
public class PurityClassTypeVariableArgument {
  static int count;

  static class Base {
    @SideEffectFree
    public int apply(Supplier<Integer> s) {
      return s.get();
    }
  }

  interface I<T> {
    @SideEffectFree
    int apply(T t);
  }

  /**
   * Sub inherits Base.apply as its implementation of I<Supplier<Integer>>.apply, but a call through
   * I<Supplier<Integer>> checks nothing about its argument.
   */
  // :: error: [purity.inherited.functional.parameter]
  static class Sub extends Base implements I<Supplier<Integer>> {}

  static class DirectOverride implements I<Supplier<Integer>> {
    @Override
    @SideEffectFree
    public int apply(Supplier<Integer> s) {
      // :: error: [purity.call]
      return s.get();
    }
  }

  void callThroughInterface(I<Supplier<Integer>> i) {
    i.apply(() -> count++);
    i.apply(() -> 1);
  }

  static class Box<T> {
    @SideEffectFree
    Box(T t) {}

    @SideEffectFree
    void put(T t) {}

    @SafeVarargs
    @SideEffectFree
    final void putAll(T... ts) {}
  }

  @SideEffectFree
  static <T> T id(T t) {
    return t;
  }

  void others(Box<Runnable> b) {
    java.util.List<Runnable> l = java.util.List.of(() -> count++);
    Runnable r = id(() -> count++);
    b.put(() -> count++);
    b.putAll(() -> count++);
    new Box<Runnable>(() -> count++);
    new Box<Runnable>(() -> count++) {};
  }

  static class SubBox extends Box<Runnable> {
    SubBox() {
      super(() -> {});
    }

    @Override
    @SideEffectFree
    void put(Runnable r) {
      // Box.put's parameter is T, so a call through Box guarantees nothing about r.
      // :: error: [purity.call]
      r.run();
    }

    void self() {
      // The call is to SubBox.put, whose parameter is a functional interface.
      // :: error: [purity.assign.field]
      put(() -> count++);
    }
  }

  void callWithSub() {
    callThroughInterface(new Sub());
  }
}
