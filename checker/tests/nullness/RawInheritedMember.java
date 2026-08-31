// A member that a raw type inherits from a supertype is erased, just like a member declared in
// the raw type itself.  The supertypes of a raw type C are the erasures of the supertypes of any
// parameterization of C (JLS 4.8), so an inherited member is a member of a *raw* supertype, and
// its type is therefore the erasure of its declared type.  javac agrees; without the
// @SuppressWarnings below it warns "unchecked call to put(Desc<String>) as a member of the raw
// type Super" for the last call.

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawInheritedMember {

  static class Desc<T> {}

  static class Super<K> {
    void put(Desc<@Nullable String> d) {}

    Desc<@Nullable String> get() {
      throw new RuntimeException();
    }
  }

  static class Sub<K> extends Super<K> {}

  // The member is declared in the receiver's own class.
  void declaredInReceiverClass(Super raw, Desc<String> d) {
    raw.put(d);
    Desc<String> x = raw.get();
  }

  // The member is inherited from Super, a raw supertype of the raw type Sub, so javac erases its
  // signature too.
  void inheritedFromRawSupertype(Sub raw, Desc<String> d) {
    raw.put(d);
    Desc<String> x = raw.get();
  }
}
