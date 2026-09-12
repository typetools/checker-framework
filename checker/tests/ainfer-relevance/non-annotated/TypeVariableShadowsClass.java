import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A type parameter shadows any class of the same name, whether the class is in `java.lang`, in the
// same package, or imported.  Inference must resolve the name "Number" to a type parameter --
// whose upper bound `CharSequence` is relevant -- rather than to `java.lang.Number` or to the
// top-level class `Number`, both of which are irrelevant.  If inference resolves the name
// incorrectly and therefore discards the annotation, then the second (validation) pass of this
// test issues the warnings that are written below.
public class TypeVariableShadowsClass<Number extends CharSequence> {

  Number field;

  void assignField(@AinferSibling1 Number n) {
    // :: warning: [assignment]
    field = n;
  }

  void useField() {
    // :: warning: [argument]
    expectsSibling1(field);
  }

  // The method's type parameter shadows the class's type parameter and both classes named
  // "Number".
  static <Number extends CharSequence> void expectsSibling1(@AinferSibling1 Number n) {}
}
