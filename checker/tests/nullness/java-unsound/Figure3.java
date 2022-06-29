public class Figure3 {
  static class Type<A> {
    class Constraint<B extends A> extends Type<B> {}

    <B> Constraint<? super B> bad() {
      // :: error: (return)
      return null;
    }

    <B> A coerce(B b) {
      // type of expression: capture#703[ extends @Initialized @Nullable Object super B[
      // extends @Initialized @Nullable Object super @Initialized @NonNull Void]]
      // method return type: A[ extends @Initialized @Nullable Object super @Initialized
      // @NonNull Void]
      return pair(this.<B>bad(), b).value;
    }
  }

  static class Sum<T> {
    Type<T> type;
    T value;

    Sum(Type<T> t, T v) {
      type = t;
      value = v;
    }
  }

  static <T> Sum<T> pair(Type<T> type, T value) {
    return new Sum<T>(type, value);
  }

  static <T, U> U coerce(T t) {
    Type<U> type = new Type<U>();
    return type.<T>coerce(t);
  }

  public static void main(String[] args) {
    String zero = Figure3.<Integer, String>coerce(0);
  }
}
