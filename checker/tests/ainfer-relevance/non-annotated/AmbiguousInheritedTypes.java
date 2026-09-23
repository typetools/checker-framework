import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A class does not inherit a member type through a supertype that declares a member type with the
// same name, even if that declaration is not itself inherited:  `Hides.Foo` hides `Relevant.Foo`,
// and `Hides.Foo` is private and therefore is not inherited by `Use`.  Thus, `Use` inherits
// exactly one member type named "Foo", and the name "Foo" in `Use` refers to `Irrelevant.Foo`,
// which is irrelevant because it is not a subtype of any type in the checker's
// `@RelevantJavaTypes`.
//
// Inference searches the supertypes of `Use` in breadth-first order, and `Relevant` and
// `Irrelevant` are the same distance from `Use`.  If inference did not model hiding by a member
// type that is not inherited, then it would find both `Relevant.Foo` and `Irrelevant.Foo`, neither
// of which hides the other, so it would have to be conservative and assume that the type might be
// relevant.
//
// Inference must discard the annotation on `field`, as the goal file shows.  If inference instead
// treats the name as ambiguous, or resolves it to `Relevant.Foo`, then it writes the annotation.
//
// Writing `@AinferSibling1` on an `Irrelevant.Foo` is itself a relevance violation, which this
// test suppresses because the warning would appear in both passes of the test and the annotated
// copy of this file contains no expected diagnostics.
public class AmbiguousInheritedTypes {

  static class Relevant {
    abstract static class Foo implements CharSequence {}
  }

  static class Hides extends Relevant {
    private static class Foo {}
  }

  interface Irrelevant {
    class Foo {}
  }

  interface SubIrrelevant extends Irrelevant {}

  static class Use extends Hides implements SubIrrelevant {

    Foo field;

    void assignField() {
      field = getSibling1();
    }

    void useField() {
      expectsSibling1(field);
    }

    @SuppressWarnings("anno.on.irrelevant")
    void expectsSibling1(@AinferSibling1 Foo f) {}

    @SuppressWarnings("anno.on.irrelevant")
    @AinferSibling1 Foo getSibling1() {
      return null;
    }
  }
}
