// TODO: Remove this `@skip-test` when the bug described below is fixed.  The crash cannot
// be written as an expected diagnostic: the test framework reports it at line -1, and its
// message contains a varying object identity hash.
// @skip-test until the bug is fixed
// A generic method invoked on a receiver whose type is a type variable whose bound is raw.  The
// method is a member of the raw bound, so javac erases its signature; it warns "unchecked call to
// <T>get(Desc<T>) as a member of the raw type Backend" for both calls below.
//
// The receiver's type is a TYPEVAR (a declared type variable in `typeVariableReceiver`, and a
// capture variable in `capturedWildcardReceiver`), not a DECLARED type, so a rawness test that
// accepts only declared types must first look through the type variable to its upper bound.
// AnnotatedTypes.asMemberOf already does that, so only the type argument inference of the diamond
// fails, crashing with "StructuralEqualityComparer: unexpected combination:  type1: [DECLARED ...]
// String  type2: [TYPEVAR ...] T extends Object".

import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawTypeVariableReceiver {

  interface Ser<T> {}

  static class Desc<T> {
    Desc(Ser<T> s) {}
  }

  interface Backend<K> {
    <T> void get(Desc<T> d);
  }

  // The bound is parameterized, so nothing is erased.
  <B extends Backend<Object>> void parameterizedBound(B b, Ser<String> ser) {
    b.get(new Desc<>(ser));
  }

  <B extends Backend> void typeVariableReceiver(B raw, Ser<String> ser) {
    raw.get(new Desc<>(ser));
  }

  // The type of `l.get(0)` is the capture of `? extends Backend`, a type variable whose upper
  // bound is the raw type `Backend`.
  void capturedWildcardReceiver(List<? extends Backend> l, Ser<String> ser) {
    l.get(0).get(new Desc<>(ser));
  }
}
