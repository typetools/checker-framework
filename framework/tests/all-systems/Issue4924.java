public class Issue4924 {
  interface Callback<A> {}

  static class Template<B> {
    class Adapter implements Callback<B> {}
  }

  static class Super<C> extends Template<C> {}

  static class Issue extends Super<String> {
    void foo() {
      Callback<String> f = new Adapter();
    }
  }
}
