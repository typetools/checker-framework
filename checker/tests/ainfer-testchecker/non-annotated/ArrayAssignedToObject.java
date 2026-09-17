// A test that WPI computes the least upper bound of all the assignments to a field, even when
// the right-hand side of an assignment has a different type kind (here, an array type) than the
// declared type of the field.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class ArrayAssignedToObject {

  private Object field;

  void assignDeclaredType(@AinferSibling2 Object o) {
    field = o;
  }

  // The kind of the right-hand side is ARRAY, whereas the kind of the type of `field` is
  // DECLARED.  The type inferred for `field` must still be the least upper bound of the types
  // of the two right-hand sides, which is @AinferParent.
  void assignArrayType(String @AinferSibling1 [] a) {
    field = a;
  }

  void testField() {
    // :: warning: [argument]
    expectsParent(field);
  }

  void expectsParent(@AinferParent Object p) {}
}
