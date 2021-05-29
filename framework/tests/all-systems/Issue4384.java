public abstract class Issue4384 {

  interface A<T extends B<T>> extends C<T>, D<T> {}

  interface C<T> {}

  interface D<T extends E<T>> {
    void f();
  }

  interface B<T extends B<T>> extends E<T> {}

  interface E<T extends E<T>> {}

  void g(A<?> t) {
    t.f();
    h(t);
  }

  abstract void h(A<? extends B<?>> t);
}
