import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// The member types of an anonymous class, both the ones it declares and the ones it inherits, are
// in scope only in its body.  They are not in scope in the arguments of the object creation
// expression that declares the anonymous class, even though those arguments are lexically nested
// within it.  Inference must resolve the name "Foo", which appears in such an argument, to the
// member class `AnonymousArgumentScope.Foo`, which is relevant because it is a subtype of
// `CharSequence`, rather than to `Base.Foo`, which the anonymous class inherits and which is
// irrelevant.  If inference resolves the name incorrectly and therefore discards the annotation,
// then the second (validation) pass of this test issues the warning that is written below.
//
// The same is true of the annotations and the type parameter section of a local class or of a
// member of an unnameable class, but inference writes no annotation in either location, so no
// goal file can show that inference resolves those names correctly.
public class AnonymousArgumentScope {

  abstract static class Foo implements CharSequence {}

  static class Base {
    static class Foo {}

    Base(Object argument) {}
  }

  static Object anonymous =
      new Base(
          new Object() {

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
          }) {};
}
