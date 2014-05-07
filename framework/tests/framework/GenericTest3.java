// Test case for Issue 133:
// http://code.google.com/p/checker-framework/issues/detail?id=133
// Upper bound of wildcard depends on declared bound of type variable.
class GenericTest3 {
  interface Foo {}
  interface Bar<T extends Foo> { T get(); }

  public void test(Bar<?> bar) {
    Foo foo = bar.get();
  }
}
