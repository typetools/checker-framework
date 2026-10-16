import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A member type of an anonymous class shadows a type of the same name that is declared elsewhere,
// such as a member of an enclosing class.  The name "Foo" refers to the member of the anonymous
// class, which is relevant because it is a subtype of `CharSequence`, rather than to the member
// class `AnonymousTypeShadows.Foo`, which is irrelevant.  An anonymous class has no name, so
// inference cannot determine the member type; it must conservatively retain the annotation.  If
// inference instead resolves the name to `AnonymousTypeShadows.Foo` and therefore discards the
// annotation, then the second (validation) pass of this test issues the warnings that are written
// below.
//
// The body of an enum constant also declares an anonymous class, so it is tested the same way.
public class AnonymousTypeShadows {

  static class Foo {}

  static Object anonymous =
      new Object() {

        abstract class Foo implements CharSequence {}

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

  enum EnumWithConstantBody {
    CONSTANT {

      abstract class Foo implements CharSequence {}

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
}
