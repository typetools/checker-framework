public class Issue5006 {

  static class C<T> {
    T get() {
      throw new RuntimeException("");
    }
  }

  interface X {
    C<? extends Object> get();
  }

  interface Y extends X {
    @Override
    // :: error: (super.wildcard)
    C<? super Object> get();
  }
}
