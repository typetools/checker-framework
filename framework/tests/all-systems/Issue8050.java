// Test case for https://github.com/typetools/checker-framework/issues/8050

// For an anonymous class created by a qualified class instance creation expression,
// `constructorFromUse` stores the type of the qualifier as the enclosing type, whereas the
// underlying javac type is enclosed by the class that lexically contains the expression.  When
// those two have different nesting depths, AnnotatedDeclaredType#getErased() used to crash while
// walking the two enclosing-type chains in lockstep.

public class Issue8050 {

  abstract static class Outer {
    abstract class Inner {}
  }

  private Outer fn = makeOuter();

  static native Outer makeOuter();

  class Getter {
    Outer.Inner get() {
      return fn.new Inner() {};
    }
  }

  // The same qualified new, but in the top-level class itself, so both enclosing chains have
  // depth 2 and no mismatch arises.  This must keep working: the bug is the differing depths,
  // not the qualified new of an anonymous class.
  Outer.Inner getInTopLevel() {
    return fn.new Inner() {};
  }

  // The qualifier is a subtype of nothing in the anonymous class's enclosing chain, and the
  // lexically enclosing chain is two levels deeper than the qualifier's.
  class Mid {
    class Deep {
      Outer.Inner get() {
        return makeOuter().new Inner() {};
      }
    }
  }

  // A generic qualifier.  Type-variable resolution for the superclass's members relies on the
  // qualifier being stored as the enclosing type, so the chains necessarily differ here.
  static class Generic<T> {
    abstract class Inner {}
  }

  static native <T> Generic<T> makeGeneric();

  class GenericGetter {
    Object get() {
      return Issue8050.<String>makeGeneric().new Inner() {};
    }
  }

  // An anonymous class created inside another anonymous class.
  Object nestedAnon =
      new Object() {
        Object g() {
          return makeOuter().new Inner() {};
        }
      };

  // The qualifier is itself an inner class, so its enclosing chain is exactly as deep as the
  // lexically enclosing one and the two agree at depth 3.
  class Encl {
    abstract class Nested {}
  }

  class EnclGetter {
    Object get(Encl e) {
      return e.new Nested() {};
    }
  }

  // An anonymous class with an implicit receiver, at two nesting depths: getReceiverType returns
  // the implicit "this" rather than an explicit qualifier.
  class Simple {}

  Object implicitReceiver() {
    return new Simple() {};
  }

  class ImplicitDeep {
    Object implicitReceiverDeeper() {
      return new Simple() {};
    }
  }

  // Non-anonymous qualified new: the qualifier really is the enclosing type.
  static class Concrete {
    class In {}
  }

  static native Concrete makeConcrete();

  class NamedGetter {
    Concrete.In get() {
      return makeConcrete().new In();
    }
  }
}
