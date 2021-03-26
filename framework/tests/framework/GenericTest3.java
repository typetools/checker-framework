// Test case for Issue 133:
// https://github.com/typetools/checker-framework/issues/133
// Upper bound of wildcard depends on declared bound of type variable.
public class GenericTest3 {
  interface Foo {}

  interface Bar<T extends Foo> {
    T get();
  }

  public void test(Bar<?> bar) {
    Foo foo = bar.get();
  }
}
