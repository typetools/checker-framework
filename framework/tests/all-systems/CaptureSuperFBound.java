// The lower bound of a captured `? super X` need not be a subtype of the declared bound of the
// type parameter, so it must not make inference fail.

public class CaptureSuperFBound {
  interface G<V extends Comparable<V>> {
    V get();
  }

  static <X extends Object> G<? super X> make(X x) {
    throw new Error();
  }

  static <Y> Y id(Y y) {
    return y;
  }

  void use(Object o, Number n, String s) {
    G<?> g1 = make(o);
    Object x = make(n).get();
    Object y = id(make(o).get());
    Comparable<?> z = id(make(s).get());
    G<? super String> g3 = make(s);
  }
}
