// A method invoked with an implicit receiver from within an inner class is erased when the class
// that javac resolves the implicit receiver to -- the innermost enclosing class that has the
// method as a member -- is raw.  That is not always the innermost enclosing class, so finding it
// requires walking outward.
//
// javac warns "unchecked call to put(Desc<String>) as a member of the raw type Sup" for the two
// `put` calls below that are not marked with an expected error, and "unchecked conversion" for the
// field read; it warns for nothing else here.
//
// The all-systems test of the same name checks that type argument inference does not crash for a
// call like these; this one checks which enclosing class the walk stops at.

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawOuterImplicitReceiver {

  static class Desc<T> {}

  static class Sup<K> {
    Desc<@Nullable String> f = new Desc<>();

    void put(Desc<@Nullable String> d) {}
  }

  // The outer class is raw and the inner class does not have `put` as a member, so the receiver is
  // `OuterRaw.this` and `put` is erased.
  static class OuterRaw extends Sup {
    class Inner {
      void implicitOuterReceiver(Desc<String> d) {
        put(d);
      }

      void implicitOuterReceiverField() {
        Desc<String> x = f;
      }
    }
  }

  // The inner class has `put` as a member, from a parameterized supertype, so the receiver is
  // `Inner.this` and nothing is erased -- even though the outer class is raw.
  static class OuterRawInnerParameterized extends Sup {
    class Inner extends Sup<Object> {
      void implicitReceiver(Desc<String> d) {
        // :: error: (argument)
        put(d);
      }
    }
  }

  // The reverse: the innermost enclosing class that has `put` as a member is the raw one, so `put`
  // is erased even though the outer class is parameterized.
  static class OuterParameterizedInnerRaw extends Sup<Object> {
    class Inner extends Sup {
      void implicitReceiver(Desc<String> d) {
        put(d);
      }
    }
  }

  // Nothing is raw.
  static class OuterParameterized extends Sup<Object> {
    class Inner {
      void implicitOuterReceiver(Desc<String> d) {
        // :: error: (argument)
        put(d);
      }
    }
  }
}
