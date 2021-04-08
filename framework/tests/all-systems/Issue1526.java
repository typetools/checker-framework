// Tet case for Issue 1526.
// https://github.com/typetools/checker-framework/issues/1526
public class Issue1526 {
  public <T> T get(T t) {
    return this.get(t);
  }

  public <T> T method(T[] t) {
    return this.method(t);
  }

  public <T> T method2(Gen<T> t) {
    return this.method2(t);
  }

  static class Gen<T> {}
}
