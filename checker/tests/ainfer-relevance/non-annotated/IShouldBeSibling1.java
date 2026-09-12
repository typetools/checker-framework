// The type factory contains custom inference logic for a class whose name is "IShouldBeSibling1"
// and for a method whose name is "iShouldBeSibling1":  the type factory infers a declaration
// annotation for the class and for the method.  The inferred annotation is a type qualifier, and
// the .ajava file writes the annotation, with the annotation's fully-qualified name, on the
// declaration.  Printing the annotation must not crash, and inference must not discard the
// annotation even though the method's return type is irrelevant:  relevance constrains the types
// on which a qualifier may be written, so it says nothing about a declaration annotation.
@SuppressWarnings("super.invocation") // Intentional.
public class IShouldBeSibling1 {

  void iShouldBeSibling1() {}

  // Unlike `void`, this method's return type `double` can bear a type qualifier, but the inferred
  // annotation is nonetheless a declaration annotation.
  double iShouldBeSibling1(double d) {
    return d;
  }
}
