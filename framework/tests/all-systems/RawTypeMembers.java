// Members of raw types (JLS 4.8), whose signatures javac erases.  Type argument inference used to
// crash on each of the shapes below because it did not detect the rawness.  Each nested class was a
// separate test file.
//
// checker/tests/nullness/RawTypeMembers.java checks the inference *results* for these shapes; this
// file only checks that they do not crash.

import java.util.List;

public class RawTypeMembers {

  // Accessing a non-static field of a raw type.  Per JLS section 4.8, the type of such a field is
  // the erasure of its declared type, so no type argument of the raw type is substituted into it.
  // Failing to erase it produced a wildcard marked "INFERENCE FAILED", which crashed capture
  // conversion.  Reduced from com.google.common.util.concurrent.AbstractFuture.
  static class RawFieldAccess {

    interface Fut<T> {}

    static class SetFuture<V> {
      final Fut<? extends V> future;

      SetFuture(Fut<? extends V> future) {
        this.future = future;
      }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void rawFieldAccess(Object localValue) {
      Fut<?> futureToPropagateTo = ((SetFuture) localValue).future;
    }
  }

  // A method inherited from a raw superclass, invoked with an implicit receiver from within an
  // inner class.  The implicit receiver is `OuterSub.this`, an instance of the *enclosing* class,
  // not of the innermost enclosing class `Inner`.  The method is a member of the raw type `Sup`, so
  // javac erases its signature; it warns "unchecked call to <T>get(Desc<T>) as a member of the raw
  // type Sup" for both calls below.
  //
  // Finding the class whose rawness matters therefore requires walking outward through the
  // enclosing classes until one has the method as a member -- the same class javac resolves the
  // implicit receiver to.  `Inner` does not have `get` as a member, so looking only at the
  // innermost enclosing class misses the erasure, and the diamond's type argument is then inferred
  // against the unerased parameter type, crashing with "StructuralEqualityComparer: unexpected
  // combination: type1: [DECLARED ...] String  type2: [TYPEVAR ...] T extends Object".
  //
  // The equivalent explicit form, `OuterSub.this.get(...)`, never crashed.
  //
  // The nested class of the same name in checker/tests/nullness/RawTypeMembers.java checks which
  // class the walk stops at.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawOuterImplicitReceiver {

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

  // A qualified class instance creation expression, `o.new Inner(...)`, whose qualifier `o` has a
  // raw type.  The constructor is a member of the raw type `Outer.Inner`, so javac erases its
  // signature; it warns "unchecked call to <T>Outer.Inner(Desc<T>) as a member of the raw type
  // Outer.Inner" for every call below except the one in `parameterizedQualifier`.
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
  static class RawQualifiedNewClass {

    interface Ser<T> {}

    static class Desc<T> {
      Desc(Ser<T> s) {}
    }

    static class Outer<K> {
      class Inner {
        <T> Inner(Desc<T> d) {}
      }
    }

    // A subclass of the raw type `Outer`.  `SubOuter` itself is not generic and is not used raw,
    // but it inherits `Inner` from a raw supertype, so the constructor is still erased.
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

  // A generic method invoked on a receiver whose type is a type variable whose bound is raw.  The
  // method is a member of the raw bound, so javac erases its signature; it warns "unchecked call to
  // <T>get(Desc<T>) as a member of the raw type Backend" for all three calls below on a raw
  // bound: every call except the one in `parameterizedBound`.
  //
  // The receiver's type is a TYPEVAR (a declared type variable in `typeVariableReceiver`, and a
  // capture variable in `capturedWildcardReceiver`), not a DECLARED type, so TypesUtils.isRawCall
  // looks through the type variable to its upper bound before testing for rawness, just as
  // AnnotatedTypes.asMemberOf does.  Otherwise type argument inference runs for the diamond against
  // the unerased parameter type `Desc<T>`, which crashes with "StructuralEqualityComparer:
  // unexpected combination:  type1: [DECLARED ...] String  type2: [TYPEVAR ...] T extends Object".
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawTypeVariableReceiver {

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

    // The upper bound is an intersection, one of whose bounds is raw.
    <B extends Backend & Cloneable> void intersectionBoundReceiver(B raw, Ser<String> ser) {
      raw.get(new Desc<>(ser));
    }
  }
}
