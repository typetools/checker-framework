// Test case for Issue #2048:
// https://github.com/typetools/checker-framework/issues/2048
//
// There are two versions:
// framework/tests/all-systems
// checker/tests/nullness
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue2048 {
  interface Foo {}

  static class Fooer<R extends Foo> {}

  class UseNbl<T> {
    void foo(Fooer<? extends T> fooer) {}
  }
  // :: error: (type.argument.type.incompatible)
  Fooer<@Nullable Foo> nblFooer = new Fooer<>();
  Fooer<@NonNull Foo> nnFooer = new Fooer<>();

  void use(UseNbl<@Nullable Foo> useNbl) {
    useNbl.foo(nblFooer);
    useNbl.foo(nnFooer);
  }

  class UseNN<T extends Object> {
    void foo(Fooer<? extends T> fooer) {}
  }
}
