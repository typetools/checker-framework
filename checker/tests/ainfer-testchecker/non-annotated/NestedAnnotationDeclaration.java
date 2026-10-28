// A constant field in a nested annotation declaration crashed ajava-based WPI.  WPI creates no
// wrapper for an annotation declaration, but the members of the annotation declaration are
// visited nonetheless.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class NestedAnnotationDeclaration {

  @interface NestedAnno {
    // A constant field, which is not an annotation element.
    String CONSTANT = "constant";

    String value();
  }

  private Object field;

  void setField(@AinferSibling1 Object o) {
    field = o;
  }

  void useField() {
    // :: warning: [assignment]
    @AinferSibling1 Object o = field;
  }
}
