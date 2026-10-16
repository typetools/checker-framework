import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A local class declaration shadows a type of the same name that is declared elsewhere, such as a
// member of an enclosing class.  Inference must resolve the name "Foo" to the local class, which
// is relevant because it is a subtype of `CharSequence`, rather than to the member class
// `LocalTypeShadows.Foo`, which is irrelevant.  If inference resolves the name incorrectly and
// therefore discards the annotation, then the second (validation) pass of this test issues the
// warning that is written below.
public class LocalTypeShadows {

  static class Foo {}

  static void declareLocalClass() {
    abstract class Foo implements CharSequence {

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
