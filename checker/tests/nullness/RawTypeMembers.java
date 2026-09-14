// The inference *results* for the raw-type shapes that
// framework/tests/all-systems/RawTypeMembers.java checks for crashes: which members a raw type
// erases (JLS 4.8), and which receivers count as raw.  Each nested class was a separate test file.

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RawTypeMembers {

  // A member of an inner class whose *enclosing* type is raw is erased, even though the inner class
  // itself declares no type parameters.  `Outer.Inner` is the raw type corresponding to
  // `Outer<K>.Inner` (JLS 4.8), and javac warns "unchecked call to put(Desc<String>) as a member of
  // the raw type Outer.Inner" for the last call below.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawEnclosingType {

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

  // A method inherited from a raw superclass is erased no matter how it is invoked: through an
  // implicit receiver, through `this`, or through `super`.  javac issues "unchecked call to
  // put(Desc<String>) as a member of the raw type Super" for all three calls below.
  //
  // For the `this` and implicit-receiver forms, the receiver's type is the subclass, so the class
  // that declares the method is found by walking up from the subclass.  For the `super` form,
  // javac's type for the receiver is the raw superclass itself, while the receiver type that
  // AnnotatedTypes.asMemberOf sees is the subclass; both must reach the same conclusion, or the
  // diamond in `superReceiverWithDiamond` has its type variable left uninstantiated.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawImplicitReceiver {

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

  // A member that a raw type inherits from a supertype is erased, just like a member declared in
  // the raw type itself.  The supertypes of a raw type C are the erasures of the supertypes of any
  // parameterization of C (JLS 4.8), so an inherited member is a member of a *raw* supertype, and
  // its type is therefore the erasure of its declared type.  javac agrees; without the
  // @SuppressWarnings below it warns "unchecked call to put(Desc<String>) as a member of the raw
  // type Super" for both `put` calls below, naming `Super` even for the receiver whose type is the
  // raw type `Sub`.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawInheritedMember {

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

  // A method invoked with an implicit receiver from within an inner class is erased when the class
  // that javac resolves the implicit receiver to -- the innermost enclosing class that has the
  // method as a member -- is raw.  That is not always the innermost enclosing class, so finding it
  // requires walking outward.
  //
  // javac warns "unchecked call to put(Desc<String>) as a member of the raw type Sup" for the two
  // `put` calls below that are not marked with an expected error, and "unchecked conversion" for
  // the field read; it warns for nothing else here.
  //
  // The nested class of the same name in framework/tests/all-systems/RawTypeMembers.java checks
  // that type argument inference does not crash for a call like these; this one checks which
  // enclosing class the walk stops at.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawOuterImplicitReceiver {

    static class Desc<T> {}

    static class Sup<K> {
      Desc<@Nullable String> f = new Desc<>();

      void put(Desc<@Nullable String> d) {}
    }

    // The outer class is raw and the inner class does not have `put` as a member, so the receiver
    // is `OuterRaw.this` and `put` is erased.
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

    // The reverse: the innermost enclosing class that has `put` as a member is the raw one, so
    // `put` is erased even though the outer class is parameterized.
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

  // A constructor invoked through a qualifier with a raw type is erased, just as a method invoked
  // on a raw receiver is.  javac warns "unchecked call to Outer.Inner(Desc<String>) as a member of
  // the raw type Outer.Inner" for every call below except the one in `parameterizedQualifier`.
  //
  // The nested class of the same name in framework/tests/all-systems/RawTypeMembers.java checks
  // that type argument inference does not crash for these calls; this one checks that the
  // constructor's signature really is erased, and that it is not erased when the qualifier is
  // parameterized.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawQualifiedNewClass {

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

  // The supertypes of a raw type are erased (JLS 4.8), even when the supertype is written with type
  // arguments in the source, as `Base<T> extends Super<@Nullable String>` is here.  javac agrees:
  // for a receiver of the raw type `Base`, `get()` returns the raw type `List`, not
  // `List<@Nullable String>`.
  //
  // SupertypeFinder records this by calling setIsUnderlyingTypeRaw() on the supertypes of a raw
  // type.  The underlying javac type still has its type arguments, so a test that consults only the
  // underlying type cannot detect this case.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawSupertypeMember {

    static class SuperSuper<K> {
      List<K> get2() {
        throw new RuntimeException();
      }
    }

    static class Super<K> extends SuperSuper<K> {
      List<K> get() {
        throw new RuntimeException();
      }
    }

    static class Base<T> extends Super<@Nullable String> {}

    // Because Base is used raw, everything Sub inherits is erased, so `get()` overrides a method
    // whose return type is the raw type `List`.  The same is true of `get2()`, which Sub inherits
    // from a supertype of the type that is written with type arguments.
    static class Sub extends Base {
      @Override
      List<@NonNull String> get() {
        throw new RuntimeException();
      }

      @Override
      List<@NonNull String> get2() {
        throw new RuntimeException();
      }
    }
  }

  // A member accessed through a receiver whose type is a type variable whose bound is raw is
  // erased, just as if the receiver had the raw bound itself.  javac warns "unchecked call to
  // put(Desc<@Nullable String>) as a member of the raw type Backend" for all three calls below, and
  // "unchecked conversion ... found: Desc" for all three assignments.
  //
  // AnnotatedTypes.asMemberOf looks through a TYPEVAR (and a WILDCARD) to its upper bound before
  // testing whether the access is on a raw type, and its INTERSECTION case tests each bound that
  // supplies the member, so all three receivers below are erased.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawTypeVariableReceiver {

    static class Desc<T> {}

    interface Backend<K> {
      void put(Desc<@Nullable String> d);

      Desc<@Nullable String> get();
    }

    // The bound is parameterized, so nothing is erased.
    <B extends Backend<Object>> void parameterizedBound(B b, Desc<@Nullable String> d) {
      b.put(d);
      Desc<@Nullable String> x = b.get();
    }

    <B extends Backend> void typeVariableReceiver(B raw, Desc<String> d) {
      raw.put(d);
      Desc<String> x = raw.get();
    }

    // The type of `l.get(0)` is the capture of `? extends Backend`, a type variable whose upper
    // bound is the raw type `Backend`.
    void capturedWildcardReceiver(List<? extends Backend> l, Desc<String> d) {
      l.get(0).put(d);
      Desc<String> x = l.get(0).get();
    }

    <B extends Backend & Cloneable> void intersectionBoundReceiver(B raw, Desc<String> d) {
      raw.put(d);
      Desc<String> x = raw.get();
    }

    // The bound that supplies the member is parameterized, so nothing is erased.
    <B extends Backend<Object> & Cloneable> void parameterizedIntersectionBound(
        B b, Desc<String> d) {
      // :: error: (argument)
      b.put(d);
    }
  }
}
