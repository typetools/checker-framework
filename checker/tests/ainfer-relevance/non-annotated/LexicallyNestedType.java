import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A nested type declaration shadows a type of the same name that is declared elsewhere, such as in
// `java.lang`.  Inference must resolve the name "Number" to the nested type
// `LexicallyNestedType.Number`, which is relevant because it is a subtype of `CharSequence`, rather
// than to `java.lang.Number`, which is irrelevant.  If inference resolves the name incorrectly and
// therefore discards the annotation, then the second (validation) pass of this test issues the
// warning that is written below.
public class LexicallyNestedType {

  abstract static class Number implements CharSequence {}

  static Number nestedNumber;

  static void assignField() {
    nestedNumber = getSibling1();
  }

  static void useField() {
    // :: warning: [argument]
    expectsSibling1(nestedNumber);
  }

  static void expectsSibling1(@AinferSibling1 Number n) {}

  static @AinferSibling1 Number getSibling1() {
    return null;
  }
}
