// A method inherited from a raw superclass is erased no matter how it is invoked: through an
// implicit receiver, through `this`, or through `super`.  javac issues "unchecked call to
// put(Desc<String>) as a member of the raw type Super" for all three calls below.
//
// For the `this` and implicit-receiver forms, the receiver's type is the subclass, so the class
// that declares the method is found by walking up from the subclass.  For the `super` form,
// javac's type for the receiver is the raw superclass itself, while the receiver type that
// AnnotatedTypes.asMemberOf sees is the subclass; both must reach the same conclusion, or the
// diamond in `superReceiverWithDiamond` has its type variable left uninstantiated.

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawImplicitReceiver {

  interface Ser<T> {}

  static class Desc<T> {
    Desc(Ser<T> s) {}
  }

  static class Super<K> {
    void put(Desc<@Nullable String> d) {}

    <T> void generic(Desc<T> d) {}
  }

  static class Sub extends Super {

    void implicitReceiver(Desc<String> d) {
      put(d);
    }

    void thisReceiver(Desc<String> d) {
      this.put(d);
    }

    void superReceiver(Desc<String> d) {
      super.put(d);
    }

    void superReceiverWithDiamond(Ser<String> s) {
      super.generic(new Desc<>(s));
    }
  }
}
