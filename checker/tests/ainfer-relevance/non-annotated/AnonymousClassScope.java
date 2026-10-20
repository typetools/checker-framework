import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// The body of an anonymous class is not the whole object creation expression.  A name that appears
// in an argument to the constructor is resolved in the enclosing scope; the member types that the
// anonymous class declares and inherits are not in scope there.  The name "Foo" in the first test
// below refers to the member class `AnonymousClassScope.Foo`, which is relevant because it is a
// subtype of `CharSequence`, rather than to the inherited member type `Base.Foo`, which is
// irrelevant.
//
// In a qualified object creation expression `outer.new Inner() { ... }`, the name "Inner" is a
// member of the type of `outer` rather than a name that is resolved in the scope of the
// expression.  Inference cannot determine the anonymous class's supertype, so it cannot determine
// the member types that the anonymous class inherits, and it must conservatively retain the
// annotation.  In the second test below, the name "Foo" refers to the inherited member type
// `Holder.Inner.Foo` rather than to `AnonymousClassScope.Inner.Foo`, which is irrelevant.
//
// If inference resolves either name incorrectly and therefore discards the annotation, then the
// second (validation) pass of this test issues the warnings that are written below.
public class AnonymousClassScope {

  abstract static class Foo implements CharSequence {}

  static class Base {
    Base(Object argument) {}

    static class Foo {}
  }

  static Object inConstructorArgument =
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

  static class Holder {
    class Inner {
      abstract class Foo implements CharSequence {}
    }
  }

  static class Inner {
    static class Foo {}
  }

  static Object qualified =
      new Holder().new Inner() {

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
      };
}
