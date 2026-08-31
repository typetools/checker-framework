// A member of an inner class whose *enclosing* type is raw is erased, even though the inner class
// itself declares no type parameters.  `Outer.Inner` is the raw type corresponding to
// `Outer<K>.Inner` (JLS 4.8), and javac warns "unchecked call to put(Desc<String>) as a member of
// the raw type Outer.Inner" for the last call below.

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawEnclosingType {

  static class Desc<T> {}

  static class Outer<K> {
    class Inner {
      void put(Desc<@Nullable String> d) {}

      Desc<@Nullable String> get() {
        throw new RuntimeException();
      }
    }
  }

  // The enclosing type is parameterized, so nothing is erased.
  void parameterizedEnclosingType(Outer<Object>.Inner inner, Desc<@Nullable String> d) {
    inner.put(d);
  }

  void rawEnclosingType(Outer.Inner inner, Desc<String> d) {
    inner.put(d);
    Desc<String> x = inner.get();
  }
}
