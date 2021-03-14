public class WildcardInvoke {
  class Demo<T> {
    void call(T p) {}
  }

  void m() {
    Demo<?> d = null;
    d.call(null);
  }
}
