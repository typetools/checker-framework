// TODO: Remove this `@skip-test` when the bug described below is fixed.  The crash cannot
// be written as an expected diagnostic: the test framework reports it at line -1, and its
// message contains a varying object identity hash.
// @skip-test until the bug is fixed
// A qualified class instance creation expression, `o.new Inner(...)`, whose qualifier `o` has a
// raw type.  The constructor is a member of the raw type `Outer.Inner`, so javac erases its
// signature; it warns "unchecked call to <T>Outer.Inner(Desc<T>) as a member of the raw type
// Outer.Inner" for both calls below.
//
// The constructor is not a member of the qualifier's type or of any of its supertypes; it is a
// member of a class that the qualifier's type encloses.  The rawness is therefore visible only in
// the enclosing expression, not in the class being instantiated, which is the same situation as
// the `outer.super(...)` form that RawQualifiedSuperCall.java tests.
//
// Passing a diamond to the constructor crashes with "StructuralEqualityComparer: unexpected
// combination:  type1: [DECLARED ...] String  type2: [TYPEVAR ...] T extends Object", because the
// erasure is applied when the argument is checked but not when the diamond's type argument is
// inferred.

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
  static class SubOuter extends Outer {}

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
