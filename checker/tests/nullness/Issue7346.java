public class Issue7346 {
  interface Base<A, B extends A> {}

  void method(Base<?, ?> base) {}
}
