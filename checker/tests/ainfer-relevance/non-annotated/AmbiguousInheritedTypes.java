import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A class does not inherit a member type through a supertype that hides it:  `Hides.Foo` hides
// `Irrelevant.Foo`, and `Hides.Foo` is private and therefore is not inherited either.  Thus, the
// name "Foo" in `Use` refers to `Relevant.Foo`, which is relevant because it is a subtype of
// `CharSequence`.
//
// Inference searches the supertypes of `Use` in breadth-first order, and it does not model hiding
// by a member type that is not inherited.  Therefore, it finds both `Irrelevant.Foo` and
// `Relevant.Foo`, which are the same distance from `Use`.  Neither one hides the other, so
// inference must not arbitrarily choose one of them; it must be conservative and assume that the
// type might be relevant.  If inference instead resolved the name to `Irrelevant.Foo` and
// therefore discarded the annotation, then the second (validation) pass of this test would issue
// the warning that is written below.
public class AmbiguousInheritedTypes {

  static class Irrelevant {
    static class Foo {}
  }

  static class Hides extends Irrelevant {
    private static class Foo {}
  }

  interface Relevant {
    abstract class Foo implements CharSequence {}
  }

  interface SubRelevant extends Relevant {}

  static class Use extends Hides implements SubRelevant {

    Foo field;

    void assignField() {
      field = getSibling1();
    }

    void useField() {
      // :: warning: [argument]
      expectsSibling1(field);
    }

    void expectsSibling1(@AinferSibling1 Foo f) {}

    @AinferSibling1 Foo getSibling1() {
      return null;
    }
  }
}
