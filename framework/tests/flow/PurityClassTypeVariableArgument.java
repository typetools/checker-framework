import java.util.function.Supplier;
import org.checkerframework.dataflow.qual.SideEffectFree;

/** A parameter whose type is a type variable of its class is checked at the call site. */
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
   * Sub inherits Base.apply as its implementation of I<Supplier<Integer>>.apply, so a call through
   * I<Supplier<Integer>> checks its argument at the call site.
   */
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
    // :: error: [purity.assign.field]
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
    // Method type variables are not checked.
    java.util.List<Runnable> l = java.util.List.of(() -> count++);
    Runnable r = id(() -> count++);
    // Class type variables are.
    // :: error: [purity.assign.field]
    b.put(() -> count++);
    // :: error: [purity.assign.field]
    b.putAll(() -> count++);
    // :: error: [purity.assign.field]
    new Box<Runnable>(() -> count++);
    // :: error: [purity.assign.field]
    new Box<Runnable>(() -> count++) {};
  }

  static class SubBox extends Box<Runnable> {
    SubBox() {
      super(() -> {});
    }

    void self() {
      // Within Box's own subclass, T is Runnable.
      // :: error: [purity.assign.field]
      put(() -> count++);
    }
  }

  void callWithSub() {
    callThroughInterface(new Sub());
  }
}
