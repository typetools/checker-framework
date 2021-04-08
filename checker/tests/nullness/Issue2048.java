// Test case for Issue #2048:
// https://github.com/typetools/checker-framework/issues/2048
//
// There are two versions:
// framework/tests/all-systems
// checker/tests/nullness

public class Issue2048 {
  interface Foo {}

  interface Fooer<R extends Foo> {}

  class UseNbl<T> {
    // T by default is @Nullable and therefore doesn't
    // fulfill the bound of R.
    // :: error: (type.argument.type.incompatible)
    void foo(Fooer<? extends T> fooer) {}
  }

  class UseNN<T extends Object> {
    void foo(Fooer<? extends T> fooer) {}
  }
}
