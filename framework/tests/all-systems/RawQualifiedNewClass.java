// A qualified class instance creation expression, `o.new Inner(...)`, whose qualifier `o` has a
// raw type.  The constructor is a member of the raw type `Outer.Inner`, so javac erases its
// signature; it warns "unchecked call to <T>Outer.Inner(Desc<T>) as a member of the raw type
// Outer.Inner" for every call below except the first.
//
// The constructor is not a member of the qualifier's type or of any of its supertypes; it is a
// member of a class that the qualifier's type encloses, so the rawness is contributed by the
// enclosing expression rather than by the class being instantiated.  The type of the class
// instance creation expression itself records this: javac gives `o.new Inner(...)` the type
// `Outer.Inner` when `o` is raw or is a subtype of a raw type, and `Outer<Object>.Inner` when it
// is parameterized.  Testing the identifier `Inner` instead sees only the declared type
// `Outer<K>.Inner`, which is never raw, and then the diamond's type argument is inferred against
// the unerased parameter type, crashing with "StructuralEqualityComparer: unexpected combination:
// type1: [DECLARED ...] String  type2: [TYPEVAR ...] T extends Object".

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawQualifiedNewClass {

  interface Ser<T> {}

  static class Desc<T> {
    Desc(Ser<T> s) {}
  }

  static class Outer<K> {
    class Inner {
      <T> Inner(Desc<T> d) {}
    }
  }

  // A subclass of the raw type `Outer`.  `SubOuter` itself is not generic and is not used raw, but
  // it inherits `Inner` from a raw supertype, so the constructor is still erased.
  static class SubOuter extends Outer {

    // No qualifier: the enclosing instance is `this`, whose type inherits `Inner` from the raw
    // `Outer`.
    void unqualified(Ser<String> ser) {
      new Inner(new Desc<>(ser));
    }

    class Deeper {
      // The enclosing instance is `SubOuter.this`, not an instance of the innermost enclosing
      // class.
      void fromInnerClass(Ser<String> ser) {
        new Inner(new Desc<>(ser));
      }
    }
  }

  // The qualifier is parameterized, so nothing is erased.
  void parameterizedQualifier(Outer<Object> o, Ser<String> ser) {
    o.new Inner(new Desc<>(ser));
  }

  void rawQualifier(Outer o, Ser<String> ser) {
    o.new Inner(new Desc<>(ser));
  }

  void subclassOfRawQualifier(SubOuter o, Ser<String> ser) {
    o.new Inner(new Desc<>(ser));
  }
}
