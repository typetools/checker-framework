// Test case for Issue 2190.
public class Issue2190 {
  interface A<X extends B> {}

  abstract class B implements C {}

  interface C {}

  class D<T> {}

  interface I<T> {
    I<T> to(D<? extends T> x);
  }

  abstract class Z {
    void f(I<A<?>> x, D<? extends A<? extends C>> y) {
      x.to(y);
    }

    void g(I<A<? extends C>> x, D<? extends A<?>> y) {
      x.to(y);
    }
  }
}
