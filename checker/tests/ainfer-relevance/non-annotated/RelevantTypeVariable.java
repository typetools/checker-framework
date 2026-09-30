import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A use of a type variable is relevant if the type variable's upper bound is relevant.  Inference
// must not discard an annotation that it infers for such a use.
public class RelevantTypeVariable<T extends CharSequence> {

  T field;

  void assignField(@AinferSibling1 T t) {
    // :: warning: [assignment]
    field = t;
  }

  void useField() {
    // :: warning: [argument]
    expectsSibling1(field);
  }

  static <U extends CharSequence> void expectsSibling1(@AinferSibling1 U u) {}

  // Likewise when a use of the type variable is the element type of an array type.
  T[] arrayField;

  void assignArrayField(@AinferSibling1 T[] t) {
    // :: warning: [assignment]
    arrayField = t;
  }

  void useArrayField() {
    // :: warning: [argument]
    expectsSibling1(arrayField[0]);
  }
}
