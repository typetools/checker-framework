// Passes the Nullness Checker but fails with a ClassCastException!

import org.checkerframework.checker.nullness.qual.Nullable;

public class Figure6NC<Ignore> {
  static class Bind<A> {
    class Curry<B extends A> {
      A curry(B b) {
        return b;
      }
    }

    <B extends A> Curry<B> upcast(@Nullable Constraint<B> constraint) {
      return new Curry<B>();
    }

    class Constraint<B extends A> {}

    <B> A coerce(B t) {
      Constraint<? super B> constraint = null;
      return upcast(constraint).curry(t);
    }
  }

  public static void main(String[] args) {
    Bind<String> bind = new Bind<String>();
    String zero = bind.<Integer>coerce(0);
  }
}
