// Test case for Issue 1991:
// https://github.com/typetools/checker-framework/issues/1991

import java.io.Serializable;

@SuppressWarnings("all") // Check for crashes only
abstract class Issue1991Full {

  abstract void g(A obj);

  static class A {
    A(C<?, ?> c) {}
  }

  interface B extends C<D, E> {}

  interface C<X extends Comparable<? super X>, Y extends Serializable> {}

  public class E implements Serializable {}

  abstract static class D implements Comparable<D>, Serializable {}

  void f(B b) {
    g(new A(b));
  }
}
