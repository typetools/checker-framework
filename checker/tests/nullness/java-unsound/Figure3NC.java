// Passes the Nullness Checker but fails with a ClassCastException!

import org.checkerframework.checker.nullness.qual.Nullable;

public class Figure3NC {
  static class Type<A> {
    class Constraint<B extends A> extends Type<B> {}

    <B> @Nullable Constraint<? super B> bad() {
      return null;
    }

    <B> A coerce(B b) {
      // :: error: (return.type.incompatible)
      return pair(this.<B>bad(), b).value;
    }
  }

  static class Sum<T> {
    @Nullable Type<T> type;
    T value;

    Sum(@Nullable Type<T> t, T v) {
      type = t;
      value = v;
    }
  }

  static <T> Sum<T> pair(@Nullable Type<T> type, T value) {
    return new Sum<T>(type, value);
  }

  static <T, U> U coerce(T t) {
    Type<U> type = new Type<U>();
    return type.<T>coerce(t);
  }

  public static void main(String[] args) {
    String zero = Figure3NC.<Integer, String>coerce(0);
  }
}
