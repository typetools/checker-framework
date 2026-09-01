// A constructor invoked through a qualifier with a raw type is erased, just as a method invoked on
// a raw receiver is.  javac warns "unchecked call to Outer.Inner(Desc<String>) as a member of the
// raw type Outer.Inner" for every call below except the first.
//
// The all-systems test of the same name checks that type argument inference does not crash for
// these calls; this one checks that the constructor's signature really is erased, and that it is
// not erased when the qualifier is parameterized.

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawQualifiedNewClass {

  static class Desc<T> {}

  static class Outer<K> {
    class Inner {
      Inner(Desc<@Nullable String> d) {}
    }
  }

  static class SubOuter extends Outer {
    void unqualified(Desc<String> d) {
      new Inner(d);
    }
  }

  // The qualifier is parameterized, so nothing is erased.
  void parameterizedQualifier(Outer<Object> o, Desc<String> d) {
    // :: error: (argument)
    o.new Inner(d);
  }

  void rawQualifier(Outer o, Desc<String> d) {
    o.new Inner(d);
  }

  void subclassOfRawQualifier(SubOuter o, Desc<String> d) {
    o.new Inner(d);
  }
}
