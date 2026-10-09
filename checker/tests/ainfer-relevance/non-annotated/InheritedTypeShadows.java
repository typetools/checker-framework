import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A local class and an anonymous class inherit their supertype's member types, which shadow types
// of the same name that are declared elsewhere.  Inference must resolve the name "Foo" to
// `Base.Foo`, which is relevant because it is a subtype of `CharSequence`, rather than to the
// member class `InheritedTypeShadows.Foo`, which is irrelevant.  Neither a local class nor an
// anonymous class has a name that inference can look up, so inference must resolve the name by
// searching the supertype.  If inference resolves the name incorrectly and therefore discards the
// annotation, then the second (validation) pass of this test issues the warnings that are written
// below.
//
// The search of the supertype is precise rather than merely conservative:  the goal file shows
// that inference discards an annotation on `Irrelevant`, which is the inherited member type
// `Base.Irrelevant` and is irrelevant.
public class InheritedTypeShadows {

  static class Foo {}

  static class Base {
    abstract static class Foo implements CharSequence {}

    static class Irrelevant {}
  }

  static void declareLocalClass() {
    class Local extends Base {

      Foo field;

      Irrelevant irrelevantField;

      void assignField() {
        field = getSibling1();
        irrelevantField = new Irrelevant();
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

  static Object anonymous =
      new Base() {

        Foo field;

        Irrelevant irrelevantField;

        void assignField() {
          field = getSibling1();
          irrelevantField = new Irrelevant();
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
