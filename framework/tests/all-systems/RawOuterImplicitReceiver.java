// TODO: Remove this `@skip-test` when the bug described below is fixed.  The crash cannot
// be written as an expected diagnostic: the test framework reports it at line -1, and its
// message contains a varying object identity hash.
// @skip-test until the bug is fixed
// A method inherited from a raw superclass, invoked with an implicit receiver from within an inner
// class.  The implicit receiver is `OuterSub.this`, an instance of the *enclosing* class, not of
// the innermost enclosing class `Inner`.  The method is a member of the raw type `Sup`, so javac
// erases its signature; it warns "unchecked call to <T>get(Desc<T>) as a member of the raw type
// Sup" for both calls below.
//
// Finding the class that declares the method therefore requires walking outward through the
// enclosing classes until one has the declaring class as a supertype.  `Inner` does not, so
// looking only at the innermost enclosing class misses the erasure and the diamond's type argument
// is inferred against the unerased parameter type, crashing with "StructuralEqualityComparer:
// unexpected combination:  type1: [DECLARED ...] String  type2: [TYPEVAR ...] T extends Object".
//
// The equivalent explicit form, `OuterSub.this.get(...)`, does not crash.

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawOuterImplicitReceiver {

  interface Ser<T> {}

  static class Desc<T> {
    Desc(Ser<T> s) {}
  }

  static class Sup<K> {
    <T> void get(Desc<T> d) {}
  }

  // The superclass is parameterized, so nothing is erased.
  static class ParameterizedOuterSub extends Sup<Object> {
    class Inner {
      void implicitReceiver(Ser<String> ser) {
        get(new Desc<>(ser));
      }
    }
  }

  static class OuterSub extends Sup {
    class Inner {
      void implicitOuterReceiver(Ser<String> ser) {
        get(new Desc<>(ser));
      }

      void explicitOuterReceiver(Ser<String> ser) {
        OuterSub.this.get(new Desc<>(ser));
      }
    }
  }
}
