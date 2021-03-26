// Test case for Issue 437:
// https://github.com/typetools/checker-framework/issues/437

abstract class I437Bar<T> {
  private final T t;

  class Norf {
    T getT() {
      return t;
    }
  }

  I437Bar(T t) {
    this.t = t;
  }

  abstract void quux(Norf norf);
}

class I437Foo extends I437Bar<Integer> {
  I437Foo(Integer i) {
    super(i);
  }

  void quux(Norf norf) {
    Integer i = norf.getT();
  }
}
